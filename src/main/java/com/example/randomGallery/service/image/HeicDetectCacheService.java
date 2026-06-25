package com.example.randomGallery.service.image;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * HEIC 格式检测缓存服务
 * 独立为一个 Spring Bean，使 @Cacheable AOP 代理在跨 Bean 调用时正确生效。
 * ImageFormatDetector.batchPreDetect 通过注入此 Bean 来确保预热时缓存被写入。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeicDetectCacheService {

    private final RestTemplate restTemplate;

    @Cacheable(value = "heiCDetectCache", key = "#url", unless = "#result == null")
    public boolean isHEICByContentType(String url) {
        if (StrUtil.isEmpty(url)) {
            return false;
        }
        // CDN URL 快速判断
        if (url.contains("xhscdn.com")) {
            log.debug("URL规则检测命中CDN规则，判定为HEIC: {}", url);
            return true;
        }
        try {
            log.debug("发送HEAD请求检测图片格式: {}", url);
            ResponseEntity<Void> response = restTemplate.exchange(url, HttpMethod.HEAD, null, Void.class);
            String contentType = response.getHeaders().getContentType() != null
                    ? response.getHeaders().getContentType().toString().toLowerCase()
                    : "";
            boolean isHEIC = contentType.contains("image/heic") || contentType.contains("image/heif");
            log.debug("Content-Type检测结果: {} -> {}", contentType, isHEIC ? "HEIC" : "非HEIC");
            return isHEIC;
        } catch (Exception e) {
            log.warn("Content-Type检测失败: {}, 异常: {}", url, e.getMessage());
            return false;
        }
    }
}
