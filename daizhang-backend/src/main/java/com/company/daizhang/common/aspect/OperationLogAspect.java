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
@RequiredArgsConstructor
public class OperationLogAspect {
    
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
                    operationLog.setParams(StrUtil.sub(params, 0, 2000));
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
            operationLog.setErrorMsg(StrUtil.sub(e.getMessage(), 0, 2000));
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
        String ip = request.getHeader("X-Forwarded-For");
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
}
