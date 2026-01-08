package com.example.randomGallery.config;

import com.example.randomGallery.utils.StrUtils;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Environment Context to hold current environment state.
 * Breaks circular dependency between MybatisPlusConfig and CacheService.
 */
@Component
public class EnvContext {

    @Value("${config.env}")
    private String defaultEnv;

    @Getter
    @Setter
    private String currentEnv;

    @PostConstruct
    public void init() {
        this.currentEnv = StrUtils.isEmpty(defaultEnv) ? "dev" : defaultEnv;
    }
}
