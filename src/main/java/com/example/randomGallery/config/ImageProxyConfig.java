package com.example.randomGallery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "image.proxy")
public class ImageProxyConfig {

    /**
     * 代理主机，仅供服务端图片中转使用。
     */
    private String host = "192.168.10.144";

    /**
     * 代理端口，仅供服务端图片中转使用。
     */
    private Integer port = 7890;
}
