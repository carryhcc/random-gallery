package com.example.randomGallery;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class RandomGalleryApplication {

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

    @Bean
    public CommandLineRunner run() {
        return args -> {
            System.out.println("=== 数据库配置（通过 @Value 注入） ===");
            System.out.println("DB_HOST: " + dbHost);
            System.out.println("DB_PORT: " + dbPort);
            System.out.println("DB_NAME: " + dbName);
            System.out.println("DB_USER: " + dbUser);
            System.out.println("================================");
        };
    }
}
