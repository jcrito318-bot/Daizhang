package com.company.daizhang.common.aspect;

import com.company.daizhang.common.annotation.RequireAccountSetAccess;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import com.company.daizhang.module.accountset.service.AccountSetAccessService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;

/**
 * 账套访问授权切面(IDOR越权治理)
 * <p>
 * 拦截标注 {@link RequireAccountSetAccess} 的 Controller 方法,
 * 按以下优先级从方法参数中解析 accountSetId 并校验:
 * 1. 方法参数名为 "accountSetId" 的直接参数(@RequestParam Long accountSetId)
 * 2. 方法参数对象的 accountSetId 字段(如 VoucherCreateRequest.getAccountSetId())
 * 3. 方法参数对象的嵌套 request.accountSetId 字段(适配 @RequestBody DTO)
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class AccountSetAccessAspect {

    private final AccountSetAccessService accountSetAccessService;

    @Around("@annotation(com.company.daizhang.common.annotation.RequireAccountSetAccess)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        RequireAccountSetAccess annotation = method.getAnnotation(RequireAccountSetAccess.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }

        List<Long> accountSetIds = resolveAccountSetIds(method, joinPoint.getArgs());
        if (accountSetIds == null || accountSetIds.isEmpty()) {
            if (annotation.required()) {
                // fail-closed: 无法解析accountSetId时拒绝访问,防止注解被静默绕过
                log.error("IDOR校验失败: 方法{}未找到accountSetId参数,拒绝访问", method.getName());
                // Bug 7: 参数名匹配可能因缺少 -parameters 编译标志而静默失败,
                // 检测参数真实名称是否可用并告警,让运维能察觉 -parameters 缺失问题
                for (Parameter p : method.getParameters()) {
                    if (!p.isNamePresent()) {
                        log.error("IDOR参数名匹配失败: 方法{}的参数{}真实名称不可用,疑似未配置 -parameters 编译标志, "
                                        + "请确认 maven-compiler-plugin 已启用 <parameters>true</parameters>",
                                method.getName(), p.getName());
                    }
                }
                throw new BusinessException(ErrorCode.FORBIDDEN, "无法解析账套ID，拒绝访问");
            }
            // required=false: 允许无accountSetId的合法场景(如按userId查询的列表接口),此时应由Service层兜底校验
            log.warn("IDOR校验跳过: 方法{}未找到accountSetId参数(required=false)", method.getName());
            return joinPoint.proceed();
        }

        // 按注解指定的授权级别校验每个账套ID(支持批量接口传入List<Long> accountSetIds)
        for (Long accountSetId : accountSetIds) {
            if (accountSetId == null) {
                continue;
            }
            if (annotation.value() == RequireAccountSetAccess.AccessLevel.OWNER) {
                accountSetAccessService.checkOwner(accountSetId);
            } else {
                accountSetAccessService.checkAccess(accountSetId);
            }
        }

        return joinPoint.proceed();
    }

    /**
     * 从方法参数中解析 accountSetId(支持单个和批量),优先级:
     * 1. 参数名为 accountSetId 的直接参数(Long)
     * 2. 参数名为 accountSetIds 的直接参数(List<Long>,批量接口)
     * 3. 参数对象的 accountSetId 字段
     * 4. 参数对象的嵌套 request/accountSetId 字段(适配 @RequestBody 包装类)
     */
    private List<Long> resolveAccountSetIds(Method method, Object[] args) {
        if (args == null || args.length == 0) {
            return null;
        }
        Parameter[] parameters = method.getParameters();

        // 第一优先级:参数名为 accountSetId 的直接参数
        for (int i = 0; i < parameters.length; i++) {
            if ("accountSetId".equals(parameters[i].getName()) && args[i] instanceof Long) {
                return Collections.singletonList((Long) args[i]);
            }
        }

        // 第二优先级:参数名为 accountSetIds 的直接参数(List<Long>,批量接口)
        for (int i = 0; i < parameters.length; i++) {
            if ("accountSetIds".equals(parameters[i].getName()) && args[i] instanceof List) {
                try {
                    @SuppressWarnings("unchecked")
                    List<Long> ids = (List<Long>) args[i];
                    return ids;
                } catch (ClassCastException e) {
                    log.debug("accountSetIds参数类型转换失败", e);
                }
            }
        }

        // 第三优先级:参数对象的 accountSetId 字段
        for (Object arg : args) {
            if (arg == null || isPrimitiveOrWrapper(arg.getClass())) {
                continue;
            }
            Long id = readField(arg, "accountSetId");
            if (id != null) {
                return Collections.singletonList(id);
            }
        }

        // 第四优先级:参数对象的嵌套 request/accountSetId 字段(适配 @RequestBody 包装类)
        for (Object arg : args) {
            if (arg == null || isPrimitiveOrWrapper(arg.getClass())) {
                continue;
            }
            Object request = readObjectField(arg, "request");
            if (request != null) {
                Long id = readField(request, "accountSetId");
                if (id != null) {
                    return Collections.singletonList(id);
                }
            }
        }
        return null;
    }

    private Long readField(Object obj, String fieldName) {
        try {
            Class<?> clazz = obj.getClass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    Object value = field.get(obj);
                    if (value instanceof Long) {
                        return (Long) value;
                    }
                    return null;
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception e) {
            log.debug("读取字段{}失败: {}", fieldName, e.getMessage());
        }
        return null;
    }

    private Object readObjectField(Object obj, String fieldName) {
        try {
            Class<?> clazz = obj.getClass();
            while (clazz != null) {
                try {
                    Field field = clazz.getDeclaredField(fieldName);
                    field.setAccessible(true);
                    return field.get(obj);
                } catch (NoSuchFieldException e) {
                    clazz = clazz.getSuperclass();
                }
            }
        } catch (Exception e) {
            log.debug("读取对象字段{}失败: {}", fieldName, e.getMessage());
        }
        return null;
    }

    private boolean isPrimitiveOrWrapper(Class<?> clazz) {
        return clazz.isPrimitive()
                || clazz == String.class
                || Number.class.isAssignableFrom(clazz)
                || clazz == Boolean.class
                || clazz == Character.class;
    }
}
