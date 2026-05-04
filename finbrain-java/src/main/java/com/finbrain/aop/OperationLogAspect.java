package com.finbrain.aop;

import cn.hutool.core.util.IdUtil;
import cn.hutool.json.JSONUtil;
import com.finbrain.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final AuditService auditService;

    @Around("@annotation(operationLogAnnotation)")
    public Object around(ProceedingJoinPoint point, com.finbrain.annotation.OperationLog operationLogAnnotation) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        com.finbrain.entity.OperationLog logEntity = new com.finbrain.entity.OperationLog();
        logEntity.setTraceId(IdUtil.fastSimpleUUID());
        logEntity.setModule(operationLogAnnotation.module());
        logEntity.setOperation(operationLogAnnotation.operation());
        logEntity.setDescription(operationLogAnnotation.description());
        logEntity.setCreateTime(LocalDateTime.now());
        
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        logEntity.setMethod(method.getDeclaringClass().getName() + "." + method.getName());
        
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            logEntity.setRequestUrl(request.getRequestURI());
            logEntity.setRequestMethod(request.getMethod());
            logEntity.setIp(getClientIp(request));
            logEntity.setUserAgent(request.getHeader("User-Agent"));
        }
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() != null) {
            try {
                logEntity.setUserId(Long.parseLong((String) authentication.getPrincipal()));
                logEntity.setUsername((String) authentication.getPrincipal());
            } catch (Exception e) {
                logEntity.setUsername(authentication.getName());
            }
        }
        
        if (operationLogAnnotation.saveRequest()) {
            Object[] args = point.getArgs();
            Map<String, Object> params = new HashMap<>();
            String[] paramNames = signature.getParameterNames();
            
            if (paramNames != null && args != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    Object arg = args[i];
                    if (arg instanceof MultipartFile) {
                        params.put(paramNames[i], "FILE: " + ((MultipartFile) arg).getOriginalFilename());
                    } else if (arg instanceof HttpServletRequest) {
                        params.put(paramNames[i], "HttpServletRequest");
                    } else {
                        params.put(paramNames[i], arg);
                    }
                }
            }
            
            try {
                logEntity.setRequestParams(JSONUtil.toJsonStr(params));
            } catch (Exception e) {
                logEntity.setRequestParams("序列化失败");
            }
        }
        
        Object result = null;
        try {
            result = point.proceed();
            logEntity.setStatus("success");
            
            if (operationLogAnnotation.saveResponse() && result != null) {
                try {
                    logEntity.setResponseData(JSONUtil.toJsonStr(result));
                } catch (Exception e) {
                    logEntity.setResponseData("序列化失败");
                }
            }
            
        } catch (Throwable e) {
            logEntity.setStatus("failed");
            logEntity.setErrorMsg(e.getMessage());
            throw e;
        } finally {
            logEntity.setDuration(System.currentTimeMillis() - startTime);
            
            try {
                auditService.saveLog(logEntity);
            } catch (Exception e) {
                log.error("保存操作日志失败", e);
            }
        }
        
        return result;
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip;
    }
}
