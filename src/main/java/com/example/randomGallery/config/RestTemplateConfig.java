package com.example.randomGallery.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置类
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 定义 RestTemplate Bean
     *
     * @return RestTemplate 对象
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}