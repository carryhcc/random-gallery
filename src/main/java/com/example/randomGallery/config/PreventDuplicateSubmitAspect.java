package com.example.randomGallery.config;

import com.example.randomGallery.annotation.PreventDuplicateSubmit;
import com.example.randomGallery.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * 防重复提交切面
 * 用于拦截带有@PreventDuplicateSubmit注解的方法，实现防重复提交功能
 */
@Slf4j
@Aspect
@Component
public class PreventDuplicateSubmitAspect {

    /**
     * 使用ConcurrentHashMap存储请求标识，确保线程安全
     * key: 请求标识，value: 上次请求时间戳
     */
    private static final ConcurrentHashMap<String, Long> REQUEST_CACHE = new ConcurrentHashMap<>();

    /**
     * 切点：拦截带有@PreventDuplicateSubmit注解的方法
     */
    @Pointcut("@annotation(com.example.randomGallery.annotation.PreventDuplicateSubmit)")
    public void preventDuplicateSubmitPointCut() {}

    /**
     * 环绕通知：处理防重复提交逻辑
     */
    @Around("preventDuplicateSubmitPointCut()")
    public Object aroundAdvice(ProceedingJoinPoint joinPoint) throws Throwable {
        // 获取方法签名
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        Method method = methodSignature.getMethod();
        
        // 获取防重复提交注解
        PreventDuplicateSubmit annotation = method.getAnnotation(PreventDuplicateSubmit.class);
        if (annotation == null) {
            return joinPoint.proceed();
        }
        
        // 获取请求对象
        HttpServletRequest request = getHttpServletRequest();
        if (request == null) {
            return joinPoint.proceed();
        }
        
        // 生成请求标识（基于用户IP、请求路径、方法名）
        String requestKey = generateRequestKey(request, methodSignature);
        long currentTime = System.currentTimeMillis();
        long interval = annotation.interval();
        
        // 检查是否在间隔时间内重复提交
        Long lastRequestTime = REQUEST_CACHE.putIfAbsent(requestKey, currentTime);
        if (lastRequestTime != null && (currentTime - lastRequestTime < interval)) {
            log.warn("重复提交检测: {} 时间间隔不足，上次提交时间: {}, 当前时间: {}", 
                     requestKey, lastRequestTime, currentTime);
            // 返回错误信息
            return Result.error(429, annotation.message());
        }
        
        try {
            // 执行原方法
            Object result = joinPoint.proceed();
            
            // 更新缓存中的时间戳
            REQUEST_CACHE.put(requestKey, currentTime);
            
            return result;
        } catch (Throwable throwable) {
            // 如果执行失败，移除缓存，允许重新提交
            REQUEST_CACHE.remove(requestKey);
            throw throwable;
        }
    }

    /**
     * 生成请求标识
     * @param request HTTP请求对象
     * @param methodSignature 方法签名
     * @return 请求标识
     */
    private String generateRequestKey(HttpServletRequest request, MethodSignature methodSignature) {
        // 包含用户IP、请求路径、HTTP方法、控制器方法名
        return request.getRemoteAddr() + ":" +
                request.getRequestURI() + ":" +
                request.getMethod() + ":" +
                methodSignature.getDeclaringType().getName() + "." +
                methodSignature.getName();
    }

    /**
     * 获取当前HTTP请求对象
     * @return HTTP请求对象，如果不存在则返回null
     */
    private HttpServletRequest getHttpServletRequest() {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (requestAttributes instanceof ServletRequestAttributes) {
            return ((ServletRequestAttributes) requestAttributes).getRequest();
        }
        return null;
    }

    /**
     * 清理过期的缓存记录（可以在系统初始化时启动一个定时任务）
     * 此方法可以定期调用，清理过期的缓存数据，避免内存泄漏
     */
    public void cleanExpiredCache() {
        long currentTime = System.currentTimeMillis();
        REQUEST_CACHE.forEach((key, timestamp) -> {
            // 清理超过5分钟的缓存记录
            if (currentTime - timestamp > TimeUnit.MINUTES.toMillis(5)) {
                REQUEST_CACHE.remove(key);
            }
        });
    }
}
