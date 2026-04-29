package com.bisai.aspect;

import com.bisai.entity.OperationLog;
import com.bisai.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/**
 * 操作日志自动记录切面
 * 在所有 Controller 方法执行后自动记录操作日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private final OperationLogMapper operationLogMapper;

    /**
     * 定义切点：匹配所有 Controller 包下的方法
     */
    @Pointcut("execution(* com.bisai.controller..*.*(..))")
    public void controllerPointcut() {
    }

    /**
     * 环绕通知：在 Controller 方法执行后记录操作日志
     */
    @Around("controllerPointcut()")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 先执行目标方法
        Object result = joinPoint.proceed();

        // 异步记录日志，避免影响业务逻辑
        try {
            recordLog(joinPoint);
        } catch (Exception e) {
            log.warn("记录操作日志失败: {}", e.getMessage());
        }

        return result;
    }

    /**
     * 记录操作日志
     */
    private void recordLog(ProceedingJoinPoint joinPoint) {
        // 获取当前请求信息
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();

        // 获取当前用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = "匿名用户";
        Long userId = null;

        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            userId = (Long) authentication.getPrincipal();
            // 从请求属性中获取用户名（由 JwtAuthenticationFilter 设置）
            username = (String) request.getAttribute("username");
            if (username == null) {
                username = "用户ID:" + userId;
            }
        }

        // 构建 action：HTTP 方法名 + 请求路径
        String httpMethod = request.getMethod();
        String requestPath = request.getRequestURI();
        String action = httpMethod + " " + requestPath;

        // 构建 description
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getSignature().getDeclaringTypeName();
        className = className.substring(className.lastIndexOf('.') + 1);
        String description = "用户" + username + "执行了" + className + "." + methodName + "操作";

        // 获取客户端 IP
        String ip = getClientIp(request);

        // 构建并保存操作日志
        OperationLog operationLog = new OperationLog();
        operationLog.setUserId(userId);
        operationLog.setUsername(username);
        operationLog.setAction(action);
        operationLog.setDescription(description);
        operationLog.setIp(ip);
        operationLog.setRequestPath(requestPath);
        operationLog.setRequestMethod(httpMethod);
        operationLog.setCreatedAt(LocalDateTime.now());

        operationLogMapper.insert(operationLog);
    }

    /**
     * 获取客户端真实 IP 地址
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多个代理时取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
