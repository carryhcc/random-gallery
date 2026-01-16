package com.example.randomGallery.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * SPA 路由过滤器
 * 将所有非 API、非静态资源的请求转发到 index.html
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class SpaRoutingFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String path = httpRequest.getRequestURI();

        // 如果是 API 请求或静态资源，直接放行
        if (path.startsWith("/api/") ||
                path.contains(".") ||
                path.equals("/")) {
            chain.doFilter(request, response);
            return;
        }

        // 其他路径转发到 index.html (SPA 路由)
        request.getRequestDispatcher("/index.html").forward(request, response);
    }
}
