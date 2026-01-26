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
        // 使用 Apache HttpClient 5
        org.apache.hc.client5.http.config.RequestConfig requestConfig = org.apache.hc.client5.http.config.RequestConfig
                .custom()
                .setConnectTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(30))
                .setResponseTimeout(org.apache.hc.core5.util.Timeout.ofSeconds(60))
                .build();

        org.apache.hc.client5.http.impl.classic.CloseableHttpClient httpClient = org.apache.hc.client5.http.impl.classic.HttpClients
                .custom()
                .setDefaultRequestConfig(requestConfig)
                .build();

        org.springframework.http.client.HttpComponentsClientHttpRequestFactory factory = new org.springframework.http.client.HttpComponentsClientHttpRequestFactory(
                httpClient);

        return new RestTemplate(factory);
    }
}