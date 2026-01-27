package com.example.randomGallery.service.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 图片下载服务
 * 
 * <p>
 * 负责从远程服务器下载图片到内存，处理常见的网络问题和CDN限制。
 * 主要功能：
 * <ul>
 * <li>下载远程图片到字节数组</li>
 * <li>自动重试机制（默认3次）</li>
 * <li>设置合适的HTTP请求头以绕过CDN限制</li>
 * </ul>
 * 
 * @author random-gallery
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageDownloadService {

    private final RestTemplate restTemplate;

    /**
     * 重试配置
     */
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 500;

    /**
     * 下载图片到内存
     * 
     * <p>
     * 该方法会自动重试最多3次，每次重试间隔500ms。
     * 使用特定的HTTP请求头来绕过CDN的限制。
     * 
     * @param url 图片的URL地址
     * @return 图片的字节数组，下载失败时返回 null
     */
    public byte[] download(String url) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                log.debug("开始下载图片 [尝试 {}/{}]: {}", attempt, MAX_RETRIES, url);

                HttpEntity<?> requestEntity = buildHttpHeaders();

                ResponseEntity<byte[]> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        requestEntity,
                        byte[].class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    byte[] imageData = response.getBody();
                    log.debug("图片下载成功，大小: {} bytes", imageData.length);
                    return imageData;
                } else {
                    log.warn("下载失败 [尝试 {}/{}]，HTTP状态码: {}",
                            attempt, MAX_RETRIES, response.getStatusCode());
                }

            } catch (Exception e) {
                lastException = e;
                log.warn("下载异常 [尝试 {}/{}]: {}", attempt, MAX_RETRIES, e.getMessage());

                // 如果还有重试机会，则等待后继续
                if (attempt < MAX_RETRIES) {
                    try {
                        Thread.sleep(RETRY_DELAY_MS);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("下载重试被中断", ie);
                        break;
                    }
                }
            }
        }

        // 所有重试都失败
        log.error("图片下载失败，已重试 {} 次: {}", MAX_RETRIES, url, lastException);
        return null;
    }

    /**
     * 构建HTTP请求头
     * 
     * <p>
     * 设置必要的请求头来绕过CDN限制：
     * <ul>
     * <li>User-Agent: 模拟Chrome浏览器</li>
     * <li>Referer: 设置为官网</li>
     * <li>Accept: 声明接受的图片格式</li>
     * </ul>
     * 
     * @return 包含必要请求头的 HttpEntity
     */
    private HttpEntity<?> buildHttpHeaders() {
        HttpHeaders headers = new HttpHeaders();

        // 模拟浏览器请求
        headers.set("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");

        // 设置 Referer 绕过CDN限制
        headers.set("Referer", "https://www.xiaohongshu.com/");

        // 声明接受的图片格式
        headers.set("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");

        return new HttpEntity<>(headers);
    }
}
