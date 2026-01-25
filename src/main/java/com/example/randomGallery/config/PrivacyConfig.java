package com.example.randomGallery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 隐私模式配置，支持运行时动态修改
 */
@Data
@Component
@ConfigurationProperties(prefix = "image.safe-mode")
public class PrivacyConfig {
    /**
     * 是否启用安全图片模式
     */
    private Boolean enabled = false;

    /**
     * 占位图URL
     */
    private String placeholderUrl;
}
