package com.example.randomGallery.utils;

import com.example.randomGallery.server.CacheService;
import jakarta.annotation.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.sql.SQLException;

@Component // 需要将此类标记为Spring组件
public class JobUtils {

    @Resource
    private CacheService cacheService;

    // 直接在方法上使用@Scheduled注解
    @Scheduled(fixedRate = 10 * 60 * 1000)
    public void switchToDevEnvironment() {
        try {
            cacheService.switchSqlName("dev");
            System.out.println("定时任务：已切换到dev环境");
        } catch (SQLException e) {
            // 建议记录日志而不是简单打印
            System.err.println("切换环境失败: " + e.getMessage());
            throw new RuntimeException("数据库环境切换失败", e);
        }
    }
}