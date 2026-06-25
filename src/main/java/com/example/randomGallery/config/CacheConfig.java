package com.example.randomGallery.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager() {
        SimpleCacheManager manager = new SimpleCacheManager();
        manager.setCaches(List.of(
                buildCache("authors", 1, 24),
                buildCache("tags", 1, 24),
                buildCache("gifIds", 1, 24),
                buildCache("heiCDetectCache", 5000, 24),
                buildWeightedCache("heiCConvertCache", 100L * 1024 * 1024, 1)
        ));
        return manager;
    }

    private CaffeineCache buildCache(String name, int maxSize, long ttlHours) {
        return new CaffeineCache(name,
                Caffeine.newBuilder()
                        .maximumSize(maxSize)
                        .expireAfterWrite(ttlHours, TimeUnit.HOURS)
                        .recordStats()
                        .build());
    }

    private CaffeineCache buildWeightedCache(String name, long maxWeightBytes, long ttlHours) {
        return new CaffeineCache(name,
                Caffeine.newBuilder()
                        .maximumWeight(maxWeightBytes)
                        .weigher((Object key, Object value) -> {
                            if (value instanceof byte[] bytes) return bytes.length;
                            return 1;
                        })
                        .expireAfterWrite(ttlHours, TimeUnit.HOURS)
                        .recordStats()
                        .build());
    }
}
