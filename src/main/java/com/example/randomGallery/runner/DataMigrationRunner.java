package com.example.randomGallery.runner;

import com.example.randomGallery.service.DataMigrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * 数据迁移启动运行器
 * 项目启动时自动检查并迁移历史数据
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataMigrationRunner implements CommandLineRunner {

    private final DataMigrationService dataMigrationService;

    @Override
    public void run(String... args) throws Exception {
        // 使用 CompletableFuture 异步执行，不阻塞项目启动
        CompletableFuture.runAsync(() -> {
            try {
                log.info("异步任务执行开始...");
//                dataMigrationService.migrateData();
                log.info("异步任务执行结束...");
            } catch (Exception e) {
                log.error("异步任务执行异常", e);
            }
        });
    }
}
