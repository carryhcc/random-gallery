package com.example.randomGallery.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 图片处理服务
 * 负责图片的格式检测、下载等通用逻辑
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final RestTemplate restTemplate;

    // 使用 Java 21 虚拟线程池执行高并发 IO 任务
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 批量检测 HEIC 图片（并发预热缓存）
     * 
     * @param urls 图片 URL 列表
     */
    public void batchDetectHeic(List<String> urls) {
        if (CollUtil.isEmpty(urls)) {
            return;
        }

        long start = System.currentTimeMillis();
        List<String> uniqueUrls = urls.stream()
                .filter(StrUtil::isNotEmpty)
                .distinct()
                .toList();

        // 并发执行检测
        List<CompletableFuture<Void>> futures = uniqueUrls.stream()
                .map(url -> CompletableFuture.runAsync(() -> {
                    try {
                        isHeicImage(url);
                    } catch (Exception e) {
                        // 忽略异常，不阻塞
                    }
                }, executor))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        log.info("Batch HEIC detect: {} urls detected in {}ms", uniqueUrls.size(), System.currentTimeMillis() - start);
    }

    /**
     * 检测 URL 是否为 HEIC 格式图片
     * 通过 HEAD 请求获取 Content-Type 来判断
     */
    @Cacheable(value = "heicDetectCache", key = "#url", unless = "#result == null")
    public boolean isHeicImage(String url) {
        if (StrUtil.isEmpty(url)) {
            return false;
        }

        if (url.contains("xhscdn.com")) {
            return true;
        }

        try {
            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.HEAD,
                    null,
                    Void.class);

            String contentType = response.getHeaders().getContentType() != null
                    ? response.getHeaders().getContentType().toString().toLowerCase()
                    : "";

            return contentType.contains("image/heic") || contentType.contains("image/heif");
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 下载图片到内存
     * 设置合适的 HTTP 头以应对 CDN 限制
     *
     * @param url 图片 URL
     * @return 图片字节数组
     */
    public byte[] downloadImage(String url) {
        int maxRetries = 3;
        int retryDelayMs = 500;
        Exception lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
                headers.set("User-Agent",
                        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36");
                headers.set("Referer", "https://www.xiaohongshu.com/");
                headers.set("Accept", "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8");

                org.springframework.http.HttpEntity<?> requestEntity = new org.springframework.http.HttpEntity<>(headers);

                ResponseEntity<byte[]> response = restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        requestEntity,
                        byte[].class);

                if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                    return response.getBody();
                } else {
                    log.warn("Download attempt {}/{} failed for {}: status {}", i + 1, maxRetries, url,
                            response.getStatusCode());
                }
            } catch (Exception e) {
                lastException = e;
                log.warn("Download attempt {}/{} error for {}: {}", i + 1, maxRetries, url, e.getMessage());

                if (i < maxRetries - 1) {
                    try {
                        Thread.sleep(retryDelayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("Download failed after {} retries: {}", maxRetries, url, lastException);
        return null;
    }

    /**
     * 检查字节数组是否为 HEIC 格式 (Magic Bytes)
     */
    public boolean isHeicBytes(byte[] data) {
        if (data == null || data.length < 12) {
            return false;
        }

        // ftyp box checking
        if (data[4] != 0x66 || data[5] != 0x74 || data[6] != 0x79 || data[7] != 0x70) {
            return false;
        }

        String majorBrand = new String(data, 8, 4);
        return majorBrand.equalsIgnoreCase("heic") ||
                majorBrand.equalsIgnoreCase("heix") ||
                majorBrand.equalsIgnoreCase("heim") ||
                majorBrand.equalsIgnoreCase("msf1") ||
                majorBrand.equalsIgnoreCase("mif1");
    }
}
