package com.example.randomGallery.service.image;

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
 * 图片格式检测服务
 * 
 * <p>
 * 提供多种方式检测图片是否为HEIC/HEIF格式：
 * <ul>
 * <li>基于URL规则的快速判断（CDN地址）</li>
 * <li>基于HTTP Content-Type的检测（带缓存）</li>
 * <li>基于Magic Bytes的字节级检测</li>
 * <li>批量预检测功能（并发执行）</li>
 * </ul>
 * 
 * @author random-gallery
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageFormatDetector {

    private final RestTemplate restTemplate;

    /**
     * 使用虚拟线程池执行并发IO任务
     */
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    /**
     * 基于URL规则快速判断是否为HEIC
     * 
     * <p>
     * CDN（xhscdn.com）的图片通常为HEIC格式，
     * 这是最快的检测方式，无需发起网络请求。
     * 
     * @param url 图片URL
     * @return 如果URL包含CDN域名返回true，否则返回false
     */
    public boolean isHEICByUrl(String url) {
        if (StrUtil.isEmpty(url)) {
            return false;
        }

        // CDN的图片通常是HEIC格式
        boolean isXhsCdn = url.contains("xhscdn.com");

        if (isXhsCdn) {
            log.debug("URL规则检测: {} 命中CDN规则，判定为HEIC", url);
        }

        return isXhsCdn;
    }

    /**
     * 基于HTTP Content-Type检测是否为HEIC（带缓存）
     * 
     * <p>
     * 通过发送HEAD请求获取Content-Type响应头来判断图片格式。
     * 检测结果会被缓存，避免重复请求。
     * 
     * @param url 图片URL
     * @return 如果Content-Type为 image/heic 或 image/heif 返回true
     */
    @Cacheable(value = "heiCDetectCache", key = "#url", unless = "#result == null")
    public boolean isHEICByContentType(String url) {
        if (StrUtil.isEmpty(url)) {
            return false;
        }

        // 先尝试URL快速检测
        if (isHEICByUrl(url)) {
            return true;
        }

        try {
            log.debug("发送HEAD请求检测图片格式: {}", url);

            ResponseEntity<Void> response = restTemplate.exchange(
                    url,
                    HttpMethod.HEAD,
                    null,
                    Void.class);

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

    /**
     * 基于Magic Bytes检测字节数组是否为HEIC格式
     * 
     * <p>
     * HEIC文件的特征：
     * <ul>
     * <li>偏移4-7: "ftyp" (文件类型标识)</li>
     * <li>偏移8-11: major brand，可能的值包括：
     * <ul>
     * <li>heic - HEIC图片</li>
     * <li>heix - HEIC图片扩展</li>
     * <li>heim - HEIC图片序列</li>
     * <li>mif1 - HEIF图片</li>
     * <li>msf1 - HEIF图片序列</li>
     * </ul>
     * </li>
     * </ul>
     * 
     * @param data 图片字节数组
     * @return 如果符合HEIC格式特征返回true
     */
    public boolean isHEICByBytes(byte[] data) {
        if (data == null || data.length < 12) {
            return false;
        }

        // 检查 ftyp box (偏移 4-7)
        if (data[4] != 0x66 || data[5] != 0x74 || data[6] != 0x79 || data[7] != 0x70) {
            return false;
        }

        // 提取 major brand (偏移 8-11)
        String majorBrand = new String(data, 8, 4);

        boolean isHEIC = majorBrand.equalsIgnoreCase("heic") ||
                majorBrand.equalsIgnoreCase("heix") ||
                majorBrand.equalsIgnoreCase("heim") ||
                majorBrand.equalsIgnoreCase("msf1") ||
                majorBrand.equalsIgnoreCase("mif1");

        if (isHEIC) {
            log.debug("Magic Bytes检测: major brand = {}, 判定为HEIC", majorBrand);
        }

        return isHEIC;
    }

    /**
     * 批量预检测HEIC图片（并发执行，预热缓存）
     * 
     * <p>
     * 使用虚拟线程并发检测多个URL，将检测结果缓存起来。
     * 这样后续访问时可以直接从缓存获取，提升性能。
     * 
     * <p>
     * 注意：检测过程中的异常会被忽略，不会影响其他URL的检测。
     * 
     * @param urls 图片URL列表
     */
    public void batchPreDetect(List<String> urls) {
        if (CollUtil.isEmpty(urls)) {
            log.debug("批量预检测: URL列表为空，跳过");
            return;
        }

        long startTime = System.currentTimeMillis();

        // 去重并过滤空值
        List<String> uniqueUrls = urls.stream()
                .filter(StrUtil::isNotEmpty)
                .distinct()
                .toList();

        log.info("开始批量预检测，共 {} 个URL", uniqueUrls.size());

        // 并发执行检测
        List<CompletableFuture<Void>> futures = uniqueUrls.stream()
                .map(url -> CompletableFuture.runAsync(() -> {
                    try {
                        // 调用检测方法，结果会被缓存
                        isHEICByContentType(url);
                    } catch (Exception e) {
                        // 忽略异常，不影响其他URL的检测
                        log.debug("预检测异常（已忽略）: {}", url);
                    }
                }, executor))
                .toList();

        // 等待所有检测完成
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long duration = System.currentTimeMillis() - startTime;
        log.info("批量预检测完成，检测 {} 个URL，耗时 {}ms", uniqueUrls.size(), duration);
    }
}
