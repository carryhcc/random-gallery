package com.example.randomGallery.service;

import jakarta.servlet.http.HttpServletRequest;

public interface VisitService {
    /**
     * 记录访问日志
     * 
     * @param request  HTTP请求
     * @param duration 耗时(ms)
     * @param status   状态码
     */
    void recordVisit(HttpServletRequest request, Long duration, Integer status);
}
