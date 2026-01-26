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
    public void convertHeicToJpeg(@RequestParam String url, HttpServletResponse response) {
        try {
            // 先尝试从缓存获取
            byte[] cachedImage = convertHeicToJpegCached(url);

            if (cachedImage == null) {
                writeError(response, "转换失败");
                return;
            }

            // 设置响应头
            response.setContentType("image/jpeg");
            response.setContentLength(cachedImage.length);
            response.setHeader("Cache-Control", "public, max-age=31536000"); // 缓存1年

            // 写入图片数据
            response.getOutputStream().write(cachedImage);
            response.getOutputStream().flush();

        } catch (Exception e) {
            log.warn("Convert failed: {}", url); // 简化日志
            try {
                writeError(response, "Error: " + e.getMessage());
            } catch (IOException ioException) {
                // ignore
            }
        }
    }

    /**
     * 带缓存的 HEIC 转 JPEG 方法
     */
    @Cacheable(value = "heicConvertCache", key = "#url", unless = "#result == null")
    public byte[] convertHeicToJpegCached(String url) {

        try {
            // 步骤 1: 下载原始图片 (复用 ImageService)
            byte[] originalImage = imageService.downloadImage(url);

            if (originalImage == null || originalImage.length == 0) {
                return null;
            }

            // 步骤 1.5: 检查图片格式
            // 如果已经是 JPEG/PNG/WebP 等浏览器可直接显示的格式，则无需转换，直接返回
            if (!imageService.isHeicBytes(originalImage)) {
                return originalImage;
            }

            log.info("Converting HEIC: {}", url); // 仅记录关键操作

            // 步骤 2: 通过 POST 方式发送给 imaginary 转换
            // 使用 Multipart 方式上传，兼容性更好
            String imaginaryUrl = "http://" + dbHost + ":6363" + "/convert?type=jpeg&quality=" + JPEG_QUALITY;
            // 构建 multipart 请求体
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

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    imaginaryUrl,
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class);

            // 步骤 3: 检查转换结果
            byte[] convertedImage = response.getBody();
            if (response.getStatusCode().is2xxSuccessful() && convertedImage != null && convertedImage.length > 0) {
                return convertedImage;
            } else {
                log.warn("imaginary status: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            log.error("Convert error: {}", url, e);
            throw new RuntimeException("Convert failed", e);
        }
    }

    private void writeError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }
}
