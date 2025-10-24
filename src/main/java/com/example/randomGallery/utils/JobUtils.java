package com.example.randomGallery.utils;

import com.example.randomGallery.server.CacheService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Component
public class JobUtils {
    
    private static final Logger log = LoggerFactory.getLogger(JobUtils.class);

    @Resource
    private CacheService cacheService;

    // 直接在方法上使用@Scheduled注解
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void switchToDevEnvironment() {
        try {
            cacheService.switchSqlName("dev");
            log.warn("定时任务：已切换到dev环境");
        } catch (SQLException e) {
            // 建议记录日志而不是简单打印
            log.error("切换环境失败: {}", e.getMessage());
            throw new RuntimeException("数据库环境切换失败", e);
        }
    }
}