package com.example.randomGallery.config;

import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置类（适配 Apache HttpClient 5.4.1 版本，无弃用API）
 */
@Configuration
public class RestTemplateConfig {

    /**
     * 定义 RestTemplate Bean，基于 HttpClient 5.4.1 实现，消除所有弃用提示
     * 连接超时30秒、响应超时60秒，与原配置功能完全一致
     *
     * @return RestTemplate 对象
     */
    @Bean
    public RestTemplate restTemplate() {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(30)) // 连接超时30秒，与原配置一致
                .build();


        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig) // 注入连接配置（含连接超时）
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofSeconds(60)) // 响应超时60秒，与原配置一致
                .build();

        CloseableHttpClient httpClient = HttpClients.custom()
                .setConnectionManager(connectionManager) // 5.4.x必选：注入连接管理器
                .setDefaultRequestConfig(requestConfig)   // 注入请求配置（含响应超时）
                .build();

        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient);
        return new RestTemplate(factory);
    }
}