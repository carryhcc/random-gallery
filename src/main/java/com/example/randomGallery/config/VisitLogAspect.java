package com.example.randomGallery.config;

import com.example.randomGallery.service.VisitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 访问日志切面（持久化到数据库）
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class VisitLogAspect {

    private final VisitService visitService;

    @Pointcut("execution(* com.example.randomGallery.controller..*(..))")
    public void controllerMethods() {
    }

    @Around("controllerMethods()")
    public Object recordVisitLog(ProceedingJoinPoint joinPoint) throws Throwable {
        long start = System.currentTimeMillis();

        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            try {
                long duration = System.currentTimeMillis() - start;
                HttpServletRequest request = getRequest();
                if (request != null) {
                    int status = 200; // 默认为200，如果要精确状态码需要检查 result 或 response
                    // 尝试获取实际状态码
                    HttpServletResponse response = getResponse();
                    if (response != null) {
                        status = response.getStatus();
                    }

                    visitService.recordVisit(request, duration, status);
                }
            } catch (Exception e) {
                log.error("触发访问日志记录失败", e);
            }
        }
    }

    private HttpServletRequest getRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }

    private HttpServletResponse getResponse() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getResponse();
        }
        return null;
    }
}
