package com.example.randomGallery.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * API 调用日志切面：
 * - 记录请求路径、方法签名、入参
 * - 执行耗时
 * - 出参
 */
@Slf4j
@Aspect
@Component
public class ApiLoggingAspect {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Pointcut("execution(* com.example.randomGallery.controller..*(..))")
    public void apiControllerMethods() {
    }

    @Around("apiControllerMethods()")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        String signature = joinPoint.getSignature().toShortString();
        String uri = getRequestUri();
        String httpMethod = getRequestMethod();

        // 跳过直接操作 HttpServletResponse 的方法（如图片转换接口）
        // 这些方法会直接写入二进制流，不能与日志框架的 JSON 序列化共存
        if (isBinaryResponseMethod(joinPoint)) {
            log.debug("[API-SKIP] {} {} -> {} (binary response, skip logging)", httpMethod, uri, signature);
            return joinPoint.proceed();
        }

        String argsJson = toSafeJson(joinPoint.getArgs());

        log.info("[API-REQ] {} {} -> {} | args={}", httpMethod, uri, signature, argsJson);

        Object result = null;
        Throwable error = null;
        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable t) {
            error = t;
            throw t;
        } finally {
            long cost = System.currentTimeMillis() - start;
            if (error == null) {
                log.info("[API-RESP] {} {} <- {} | cost={}ms | result={}", httpMethod, uri, signature, cost,
                        toSafeJson(result));
            } else {
                log.error("[API-RESP] {} {} <- {} | cost={}ms | error={}", httpMethod, uri, signature, cost,
                        error.toString());
            }
        }
    }

    /**
     * 检查是否为二进制响应方法（返回 void 且有 HttpServletResponse 参数）
     */
    private boolean isBinaryResponseMethod(ProceedingJoinPoint joinPoint) {
        // 检查方法签名
        String signature = joinPoint.getSignature().toString();

        // 检查是否为 void 返回类型
        if (!signature.startsWith("void ")) {
            return false;
        }

        // 检查参数中是否包含 HttpServletResponse
        Object[] args = joinPoint.getArgs();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof jakarta.servlet.http.HttpServletResponse) {
                    return true;
                }
            }
        }

        return false;
    }

    private String getRequestUri() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            return request.getRequestURI();
        }
        return "";
    }

    private String getRequestMethod() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            HttpServletRequest request = servletAttrs.getRequest();
            return request.getMethod();
        }
        return "";
    }

    private String toSafeJson(Object obj) {
        try {
            if (obj == null)
                return "null";
            if (obj instanceof Object[] args) {
                Object[] filtered = Arrays.stream(args)
                        .map(this::maskIfSensitive)
                        .toArray();
                return objectMapper.writeValueAsString(filtered);
            }
            return objectMapper.writeValueAsString(maskIfSensitive(obj));
        } catch (JsonProcessingException e) {
            return String.valueOf(obj);
        }
    }

    private Object maskIfSensitive(Object value) {
        if (value == null)
            return null;
        // 简单脱敏与体积控制
        if (value instanceof CharSequence str) {
            String s = str.toString();
            if (s.length() > 512) {
                return s.substring(0, 512) + "...";
            }
            return s;
        }
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> copy = new HashMap<>();
            for (Map.Entry<?, ?> e : map.entrySet()) {
                String key = String.valueOf(e.getKey());
                Object v = e.getValue();
                if (isSensitiveKey(key)) {
                    copy.put(key, "***");
                } else {
                    copy.put(key, v);
                }
            }
            return copy;
        }
        // 常见集合打印限制
        if (value instanceof Iterable<?> iterable) {
            return toLimitedList(iterable, 50);
        }
        return value;
    }

    private List<Object> toLimitedList(Iterable<?> iterable, int limit) {
        List<Object> list = new ArrayList<>();
        int i = 0;
        for (Object o : iterable) {
            if (i++ >= limit) {
                list.add("...");
                break;
            }
            list.add(maskIfSensitive(o));
        }
        return list;
    }

    private boolean isSensitiveKey(String key) {
        String lower = key.toLowerCase();
        return lower.contains("password") || lower.contains("passwd") || lower.contains("secret")
                || lower.contains("token");
    }
}
