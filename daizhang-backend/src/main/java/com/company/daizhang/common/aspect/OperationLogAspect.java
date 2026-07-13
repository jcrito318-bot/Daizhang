package com.company.daizhang.common.aspect;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.company.daizhang.common.annotation.OperationLog;
import com.company.daizhang.common.utils.SecurityUtils;
import com.company.daizhang.module.system.entity.SysOperationLog;
import com.company.daizhang.module.system.service.SysOperationLogService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.Method;
import java.time.LocalDateTime;

/**
 * 操作日志切面
 */
@Slf4j
@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final int LOG_DETAIL_MAX_LENGTH = 2000;

    private final SysOperationLogService operationLogService;
    
    @Around("@annotation(com.company.daizhang.common.annotation.OperationLog)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        SysOperationLog operationLog = new SysOperationLog();
        operationLog.setUserId(SecurityUtils.getCurrentUserId());
        operationLog.setUsername(SecurityUtils.getCurrentUsername());
        operationLog.setCreateTime(LocalDateTime.now());
        
        try {
            // 获取方法信息
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            Method method = signature.getMethod();
            String className = joinPoint.getTarget().getClass().getName();
            String methodName = method.getName();
            
            operationLog.setMethod(className + "." + methodName);
            
            // 获取操作描述
            OperationLog annotation = method.getAnnotation(OperationLog.class);
            if (annotation != null && StrUtil.isNotBlank(annotation.value())) {
                operationLog.setOperation(annotation.value());
            } else {
                operationLog.setOperation(methodName);
            }
            
            // 获取参数
            Object[] args = joinPoint.getArgs();
            if (args != null && args.length > 0) {
                try {
                    String params = JSONUtil.toJsonStr(args);
                    // 脱敏敏感字段(密码/token/密钥等),防止明文持久化到 sys_operation_log.params
                    params = maskSensitiveFields(params);
                    operationLog.setParams(StrUtil.sub(params, 0, LOG_DETAIL_MAX_LENGTH));
                } catch (Exception e) {
                    operationLog.setParams("参数序列化失败");
                }
            }
            
            // 获取IP（非HTTP上下文如定时任务/异步调用时RequestAttributes为null,需防御）
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                operationLog.setIp(getIpAddress(request));
            }
            
            // 执行方法
            Object result = joinPoint.proceed();
            
            operationLog.setStatus(1);
            operationLog.setCostTime(System.currentTimeMillis() - startTime);
            
            return result;
        } catch (Exception e) {
            operationLog.setStatus(0);
            operationLog.setErrorMsg(StrUtil.sub(e.getMessage(), 0, LOG_DETAIL_MAX_LENGTH));
            operationLog.setCostTime(System.currentTimeMillis() - startTime);
            throw e;
        } finally {
            try {
                operationLogService.saveLog(operationLog);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }
    }
    
    private String getIpAddress(HttpServletRequest request) {
        // 注意:X-Forwarded-For可被客户端伪造,生产环境应配置可信代理链
        // 当前实现取X-Forwarded-For首段,仅作审计参考
        String ip = request.getHeader("X-Forwarded-For");
        if (StrUtil.isNotBlank(ip) && !"unknown".equalsIgnoreCase(ip)) {
            // 取第一个IP并清洗(防止注入)
            ip = ip.split(",")[0].trim();
            // 校验IP格式(仅允许数字/字母/冒号/点),不合法则回退到后续Header/RemoteAddr
            if (!ip.matches("^[0-9a-fA-F:.]+$")) {
                ip = null;
            }
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StrUtil.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        
        // 多个代理的情况，第一个IP为客户端真实IP
        if (StrUtil.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }

        return ip;
    }

    /**
     * 对JSON字符串中的敏感字段值进行脱敏,替换为 "***"。
     * 兼容字段名与值之间可能存在的空格(允许冒号两侧有空白)。
     * 采用大小写不敏感的包含匹配,覆盖以下字段(及其变体,如 passwordHash/accessToken/refreshToken/apiKeySecret 等):
     * password / passwd / pwd / token / secret / apikey / api_key / credential
     * / authorization / cookie / privatekey / private_key / salt / sessionid / session_id
     */
    private String maskSensitiveFields(String params) {
        if (StrUtil.isBlank(params)) {
            return params;
        }
        // 正则匹配 "fieldName" : "value" 形式(允许冒号两侧有空白),将value替换为 ***
        // (?i) 大小写不敏感;字段名包含下列关键字之一即命中
        return params.replaceAll(
                "(?i)(\"(?:.*(?:password|passwd|pwd|token|secret|apikey|api_key|credential|authorization|cookie|privatekey|private_key|salt|sessionid|session_id).*)\"\\s*:\\s*)\"[^\"]*\"",
                "$1\"***\"");
    }
}
