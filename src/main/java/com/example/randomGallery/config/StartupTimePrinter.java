package com.example.randomGallery.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class StartupTimePrinter implements CommandLineRunner {

    private final ApplicationContext context;
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(StartupTimePrinter.class);

    public StartupTimePrinter(ApplicationContext context) {
        this.context = context;
    }

    @Override
    public void run(String... args) {
        long startTime = context.getStartupDate();
        long totalTimeMillis = System.currentTimeMillis() - startTime;
        log.info("--- 启动成功,耗时:{} ms ---", totalTimeMillis);
    }
}