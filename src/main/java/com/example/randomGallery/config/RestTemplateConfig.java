package com.example.randomGallery.config;

import org.apache.hc.client5.http.impl.routing.DefaultProxyRoutePlanner;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.util.Timeout;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * RestTemplate 配置类（适配 Apache HttpClient 5.4.1 版本，无弃用API）
 */
@Configuration
public class RestTemplateConfig {

    private final ImageProxyConfig imageProxyConfig;

    public RestTemplateConfig(ImageProxyConfig imageProxyConfig) {
        this.imageProxyConfig = imageProxyConfig;
    }

    /**
     * 定义 RestTemplate Bean，基于 HttpClient 5.4.1 实现，消除所有弃用提示
     * 连接超时30秒、响应超时60秒，与原配置功能完全一致
     *
     * @return RestTemplate 对象
     */
    @Bean
    @Primary
    public RestTemplate restTemplate() {
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(createHttpClient(null)));
    }

    @Bean("imageProxyRestTemplate")
    public RestTemplate imageProxyRestTemplate() {
        HttpHost proxyHost = new HttpHost(imageProxyConfig.getHost(), imageProxyConfig.getPort());
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(createHttpClient(proxyHost)));
    }

    private CloseableHttpClient createHttpClient(HttpHost proxyHost) {
        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(Timeout.ofSeconds(30))
                .build();

        PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder.create()
                .setDefaultConnectionConfig(connectionConfig)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setResponseTimeout(Timeout.ofSeconds(60))
                .build();

        var builder = HttpClients.custom()
                .setConnectionManager(connectionManager)
                .setDefaultRequestConfig(requestConfig);

        if (proxyHost != null) {
            builder.setRoutePlanner(new DefaultProxyRoutePlanner(proxyHost));
        }

        return builder.build();
    }
}
