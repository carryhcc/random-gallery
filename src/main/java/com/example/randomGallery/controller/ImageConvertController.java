package com.example.randomGallery.controller;

import com.example.randomGallery.service.ImageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

/**
 * 图片转换控制器 - 处理图片格式转换（如 HEIC 转 JPEG）
 */
@Slf4j
@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
public class ImageConvertController {

    private final RestTemplate restTemplate;
    private final ImageService imageService;

    @Value("${db.host}")
    private String dbHost;
    // imaginary 服务配置
    private static final int JPEG_QUALITY = 90;

    /**
     * HEIC 图片转换为 JPEG
     */
    @GetMapping("/convert-heic")
    public void convertHEICToJpeg(@RequestParam String url, HttpServletResponse response) {
        try {
            log.warn("[STEP 1] 开始转换图片: {}", url);
            log.warn("[STEP 2] URL 参数接收成功");

            // 先尝试从缓存获取
            log.warn("[STEP 3] 尝试从缓存获取图片");
            byte[] cachedImage = convertHEICToJpegCached(url);

            if (cachedImage == null) {
                log.warn("[STEP 4] ❌ 缓存获取失败，cachedImage is null");
                writeError(response, "转换失败");
                return;
            }

            log.warn("[STEP 5] ✅ 缓存获取成功，图片大小: {} bytes", cachedImage.length);

            // 设置响应头
            log.warn("[STEP 6] 设置响应头 Content-Type: image/jpeg");
            response.setContentType("image/jpeg");
            response.setContentLength(cachedImage.length);
            response.setHeader("Cache-Control", "public, max-age=31536000"); // 缓存1年

            // 写入图片数据
            log.warn("[STEP 7] 开始写入图片数据到 response");
            response.getOutputStream().write(cachedImage);
            response.getOutputStream().flush();
            log.warn("[STEP 8] ✅ 图片数据写入成功，转换完成");

        } catch (Exception e) {
            log.warn("[STEP ERROR] ❌ 转换过程发生异常: {}", url, e);
            log.warn("[STEP ERROR] 异常类型: {}", e.getClass().getName());
            log.warn("[STEP ERROR] 异常消息: {}", e.getMessage());
            try {
                writeError(response, "Error: " + e.getMessage());
            } catch (IOException ioException) {
                log.warn("[STEP ERROR] ❌ 写入错误响应失败", ioException);
            }
        }
    }

    /**
     * 带缓存的 HEIC 转 JPEG 方法
     */
    @Cacheable(value = "heiCConvertCache", key = "#url", unless = "#result == null")
    public byte[] convertHEICToJpegCached(String url) {

        try {
            // 步骤 1: 下载原始图片 (复用 ImageService)
            log.warn("[CACHE-STEP 1] 准备下载原始图片: {}", url);
            byte[] originalImage = imageService.downloadImage(url);

            if (originalImage == null || originalImage.length == 0) {
                log.warn("[CACHE-STEP 1.1] ❌ 下载失败，originalImage is null or empty");
                return null;
            }

            log.warn("[CACHE-STEP 1.2] ✅ 下载成功，原始图片大小: {} bytes", originalImage.length);

            // 步骤 1.5: 检查图片格式
            // 如果已经是 JPEG/PNG/WebP 等浏览器可直接显示的格式，则无需转换，直接返回
            log.warn("[CACHE-STEP 2] 检查图片格式是否为 HEIC");
            boolean isHEIC = imageService.isHEICBytes(originalImage);
            log.warn("[CACHE-STEP 2.1] 格式检查结果: isHEIC = {}", isHEIC);

            if (!isHEIC) {
                log.warn("[CACHE-STEP 2.2] ✅ 非 HEIC 格式，无需转换，直接返回");
                return originalImage;
            }

            log.warn("[CACHE-STEP 3] 📝 检测到 HEIC 格式，需要转换");
            log.warn("[CACHE-STEP 4] 准备调用 imaginary 服务");
            log.warn("[CACHE-STEP 4.1] DB_HOST 配置值: {}", dbHost);

            // 步骤 2: 通过 POST 方式发送给 imaginary 转换
            // 使用 Multipart 方式上传，兼容性更好
            String imaginaryUrl = "http://" + dbHost + ":6363" + "/convert?type=jpeg&quality=" + JPEG_QUALITY;
            log.warn("[CACHE-STEP 4.2] imaginary URL: {}", imaginaryUrl);

            // 构建 multipart 请求体
            log.warn("[CACHE-STEP 5] 构建 multipart 请求体");
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            ByteArrayResource resource = new ByteArrayResource(originalImage) {
                @Override
                public String getFilename() {
                    return "image.heic";
                }
            };
            body.add("file", resource);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            log.warn("[CACHE-STEP 6] 开始发送请求到 imaginary 服务");

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    imaginaryUrl,
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class);

            log.warn("[CACHE-STEP 7] ✅ imaginary 响应状态码: {}", response.getStatusCode());

            // 步骤 3: 检查转换结果
            byte[] convertedImage = response.getBody();

            if (convertedImage != null) {
                log.warn("[CACHE-STEP 8] 转换后图片大小: {} bytes", convertedImage.length);
            } else {
                log.warn("[CACHE-STEP 8] ❌ 转换后图片为 null");
            }

            if (response.getStatusCode().is2xxSuccessful() && convertedImage != null && convertedImage.length > 0) {
                log.warn("[CACHE-STEP 9] ✅ 转换成功，返回转换后的图片");
                return convertedImage;
            } else {
                log.warn("[CACHE-STEP 9] ❌ 转换失败，imaginary status: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            log.warn("[CACHE-STEP ERROR] ❌ 转换过程发生异常");
            log.warn("[CACHE-STEP ERROR] 异常类型: {}", e.getClass().getName());
            log.warn("[CACHE-STEP ERROR] 异常消息: {}", e.getMessage());
            log.error("[CACHE-STEP ERROR] 完整堆栈: {}", url, e);
            throw new RuntimeException("Convert failed", e);
        }
    }

    private void writeError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }
}
