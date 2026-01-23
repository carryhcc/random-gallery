package com.example.randomGallery.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 图片安全模式配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "image.safe-mode")
public class SafeImageProperties {
    /**
     * 是否启用安全图片模式
     */
    private Boolean enabled = false;

    /**
     * 占位图URL（用于替换真实图片URL）
     */
    private String placeholderUrl;
}
