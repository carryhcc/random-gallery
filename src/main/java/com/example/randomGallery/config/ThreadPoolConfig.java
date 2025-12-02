package com.example.randomGallery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.*;

/**
 * 线程池配置类
 */
@Configuration
public class ThreadPoolConfig {

    /**
     * 定义线程池 Bean
     *
     * @return ThreadPoolExecutor 对象
     */
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        // 获取CPU核心数
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        // 核心线程数：CPU核心数
        // 最大线程数：核心线程数的2倍
        // 线程存活时间：60秒
        // 任务队列：容量100的阻塞队列
        // 线程工厂：默认线程工厂
        // 拒绝策略：CallerRunsPolicy，确保任务不会丢失
        return new ThreadPoolExecutor(
                corePoolSize,
                corePoolSize * 2,
                60L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(100),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}