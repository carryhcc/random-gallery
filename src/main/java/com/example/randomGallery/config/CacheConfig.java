package com.example.randomGallery.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 缓存配置类
 * 使用 Caffeine 作为缓存实现，提供高性能的本地缓存支持
 */
@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * 配置缓存管理器
     * 
     * 缓存策略：
     * - authors: 作者列表缓存
     * - tags: 标签列表缓存
     * - heiCDetectCache: HEIC 检测结果缓存（URL -> Boolean）
     * - heiCConvertCache: HEIC 转换结果缓存（URL -> byte[]）
     * 
     * @return CacheManager 实例
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(
                "authors",
                "tags",
                "heiCDetectCache",
                "heiDConvertCache");

        cacheManager.setCaffeine(Caffeine.newBuilder()
                // 写入后 24 小时过期
                .expireAfterWrite(24, TimeUnit.HOURS)
                // 最大缓存 1000 条记录
                .maximumSize(1000)
                // 启用统计信息（用于监控缓存命中率）
                .recordStats());

        return cacheManager;
    }
}
