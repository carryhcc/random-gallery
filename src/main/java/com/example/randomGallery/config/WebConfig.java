package com.example.randomGallery.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC 配置 - 支持前后端分离
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

        /**
         * CORS 跨域配置
         */
        @Override
        public void addCorsMappings(@NonNull CorsRegistry registry) {
                registry.addMapping("/api/**")
                                .allowedOrigins(
                                                "http://localhost:3000", // Next.js 开发服务器
                                                "http://localhost:5173", // Vite 开发服务器（备用）
                                                "http://localhost:8086" // 生产环境同域
                                )
                                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                                .allowedHeaders("*")
                                .allowCredentials(true)
                                .maxAge(3600);
        }

        /**
         * 静态资源处理
         */
        @Override
        public void addResourceHandlers(@NonNull ResourceHandlerRegistry registry) {
                registry
                                .addResourceHandler("/**")
                                .addResourceLocations("classpath:/static/")
                                .setCachePeriod(3600)
                                .resourceChain(true);
        }

        /**
         * SPA 路由支持
         * Spring Boot 3.x 使用 PathPattern，不支持复杂的路径模式
         * 这里只配置根路径转发，其他路径由前端路由处理
         */
        @Override
        public void addViewControllers(@NonNull ViewControllerRegistry registry) {
                // 根路径转发到 index.html
                registry.addViewController("/")
                                .setViewName("forward:/index.html");
        }
}
