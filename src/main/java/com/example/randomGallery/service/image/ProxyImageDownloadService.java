package com.example.randomGallery.service.image;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * 通过受控代理下载目标图片，供站内图片中转接口使用。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProxyImageDownloadService {

    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY_MS = 500;

    @Qualifier("imageProxyRestTemplate")
    private final RestTemplate imageProxyRestTemplate;

    public ResponseEntity<byte[]> download(String url) {
        Exception lastException = null;

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                HttpEntity<Void> requestEntity = new HttpEntity<>(buildHeaders());
                ResponseEntity<byte[]> response = imageProxyRestTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        requestEntity,
                        byte[].class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return response;
                }

                log.warn("代理下载失败 [尝试 {}/{}]，状态码: {}", attempt, MAX_RETRIES, response.getStatusCode());
            } catch (Exception e) {
                lastException = e;
                log.warn("代理下载异常 [尝试 {}/{}]: {}", attempt, MAX_RETRIES, e.getMessage());
                if (attempt < MAX_RETRIES) {
                    sleepBeforeRetry();
                }
            }
        }

        log.error("代理下载图片失败，已重试 {} 次: {}", MAX_RETRIES, url, lastException);
        return null;
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
        headers.set("Referer", "https://telegra.ph/");
        headers.set("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");
        return headers;
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RETRY_DELAY_MS);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            log.error("代理下载重试被中断", interruptedException);
        }
    }

    public static MediaType resolveContentType(ResponseEntity<byte[]> responseEntity) {
        MediaType contentType = responseEntity.getHeaders().getContentType();
        return contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM;
    }
}
