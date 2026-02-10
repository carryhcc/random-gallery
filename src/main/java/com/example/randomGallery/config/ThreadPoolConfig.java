package com.example.randomGallery.config;

import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 线程池配置类
 */
@Configuration
@EnableAsync
public class ThreadPoolConfig {

    private static final int QUEUE_CAPACITY = 100;
    private static final int KEEP_ALIVE_SECONDS = 60;

    /**
     * 通用线程池 Bean（用于图片下载等任务）
     */
    @Bean
    public ThreadPoolExecutor threadPoolExecutor() {
        // 获取CPU核心数
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        return new ThreadPoolExecutor(
                corePoolSize,
                corePoolSize * 2,
                KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY),
                new CustomThreadFactory("download-pool-", true),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    /**
     * 异步任务线程池（用于下载任务等异步操作）
     */
    @Bean("taskExecutor")
    public ThreadPoolTaskExecutor taskExecutor() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(corePoolSize * 2);
        executor.setQueueCapacity(QUEUE_CAPACITY);
        executor.setKeepAliveSeconds(KEEP_ALIVE_SECONDS);
        executor.setThreadNamePrefix("async-task-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();
        return executor;
    }

    /**
     * 自定义线程工厂
     */
    private static class CustomThreadFactory implements ThreadFactory {
        private final String namePrefix;
        private final boolean daemon;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        CustomThreadFactory(String namePrefix, boolean daemon) {
            this.namePrefix = namePrefix;
            this.daemon = daemon;
        }

        @Override
        public Thread newThread(@NonNull Runnable r) {
            Thread t = new Thread(r, namePrefix + threadNumber.getAndIncrement());
            t.setDaemon(daemon);
            return t;
        }
    }
}