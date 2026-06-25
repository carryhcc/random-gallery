package com.example.randomGallery.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AdminIpInterceptor implements HandlerInterceptor {

    private final Set<String> allowedIps;

    public AdminIpInterceptor(@Value("${admin.allowed-ips:127.0.0.1,0:0:0:0:0:0:0:1,::1}") String allowedIpsConfig) {
        this.allowedIps = Arrays.stream(allowedIpsConfig.split(","))
                .map(String::trim)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ip = getClientIp(request);
        if (!allowedIps.contains(ip)) {
            log.warn("拒绝来自非授权IP的管理接口访问: {} {}", ip, request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"Access denied\"}");
            return false;
        }
        return true;
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
