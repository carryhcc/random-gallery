package com.example.randomGallery;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RandomGalleryApplication {
    
    private static final Logger log = LoggerFactory.getLogger(RandomGalleryApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(RandomGalleryApplication.class, args);
    }

    @Value("${db.host}")
    private String dbHost;

    @Value("${db.port}")
    private String dbPort;

    @Value("${db.name}")
    private String dbName;

    @Value("${db.username}")
    private String dbUser;

    @Value("${server.port}")
    private String port;

    @Bean
    public CommandLineRunner run() {
        return args -> {
            log.warn("=== 数据库配置（通过 @Value 注入） ===");
            log.warn("DB_HOST: {}", dbHost);
            log.warn("DB_PORT: {}", dbPort);
            log.warn("DB_NAME: {}", dbName);
            log.warn("DB_USER: {}", dbUser);
            log.warn("================================");
            log.warn("http://127.0.0.1:{}", port);
        };
    }
}
