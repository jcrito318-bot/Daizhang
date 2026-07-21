package com.company.daizhang.common.aspect;

import cn.hutool.core.util.StrUtil;
import com.company.daizhang.common.annotation.SensitiveOperation;
import com.company.daizhang.common.exception.BusinessException;
import com.company.daizhang.common.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;

/**
 * 敏感操作切面 (P3.4)
 * <p>
 * 拦截所有标注 {@link SensitiveOperation} 的控制器方法,在方法执行前校验请求头
 * {@code X-Confirm} 是否为 "true";不是则抛出 {@link BusinessException}(返回 400),
 * 拒绝执行后续业务逻辑。
 * <p>
 * Order 设为 {@link Ordered#HIGHEST_PRECEDENCE} + 1,略低于 OperationLogAspect(HIGHEST_PRECEDENCE)。
 * 执行顺序:OperationLogAspect(@Around,外层) → SensitiveOperationAspect(@Before,内层) → 业务方法。
 * 这样设计的好处:敏感操作被拒绝时(BusinessException),OperationLogAspect 的 catch 块会
 * 记录 status=0 的失败日志(含错误信息"敏感操作需二次确认"),便于审计追踪被拒绝的尝试,
 * 而不会记录"成功"的操作日志,业务逻辑也不会执行。
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class SensitiveOperationAspect {

    /**
     * 二次确认请求头名称。
     * 前端在 ElMessageBox.confirm 通过后,通过 axios request 拦截器添加此头。
     */
    public static final String CONFIRM_HEADER = "X-Confirm";

    @Before("@annotation(com.company.daizhang.common.annotation.SensitiveOperation)")
    public void beforeSensitiveOperation(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        SensitiveOperation annotation = method.getAnnotation(SensitiveOperation.class);
        String operationDesc = (annotation != null && StrUtil.isNotBlank(annotation.value()))
                ? annotation.value() : method.getName();

        HttpServletRequest request = getCurrentRequest();
        // 非HTTP上下文(理论上 Controller 方法不会出现,但防御性处理):一律拒绝
        if (request == null) {
            log.warn("敏感操作 [{}] 在非HTTP上下文调用,拒绝执行", operationDesc);
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "敏感操作需二次确认,请在前端确认后重试");
        }

        String confirmHeader = request.getHeader(CONFIRM_HEADER);
        if (!"true".equalsIgnoreCase(confirmHeader)) {
            // 没有该头或为 false 时返回 400 错误,拒绝执行
            log.warn("敏感操作 [{}] 未携带有效的 {} 头,拒绝执行。操作路径: {} {}",
                    operationDesc, CONFIRM_HEADER, request.getMethod(), request.getRequestURI());
            throw new BusinessException(ErrorCode.PARAM_ERROR.getCode(),
                    "敏感操作需二次确认,请在前端确认后重试");
        }

        log.info("敏感操作 [{}] 已通过二次确认,操作路径: {} {}",
                operationDesc, request.getMethod(), request.getRequestURI());
    }

    /**
     * 从 RequestContextHolder 获取当前 HttpServletRequest。
     * 非HTTP上下文(如定时任务、异步调用)返回 null。
     */
    private HttpServletRequest getCurrentRequest() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs == null ? null : attrs.getRequest();
    }
}
