package com.example.randomGallery.controller;

import com.example.randomGallery.service.ImageService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * 图片转换控制器
 * 
 * <p>
 * 提供图片格式转换的HTTP接口，主要用于将HEIC格式转换为浏览器兼容的JPEG格式。
 * 
 * <p>
 * 端点说明：
 * <ul>
 * <li>GET /api/image/convert-heic - 转换HEIC图片为JPEG</li>
 * </ul>
 * 
 * <p>
 * 缓存策略：
 * <ul>
 * <li>转换结果由Service层缓存（Spring Cache）</li>
 * <li>响应头设置1年缓存（Cache-Control: public, max-age=31536000）</li>
 * </ul>
 * 
 * @author random-gallery
 */
@Slf4j
@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
public class ImageConvertController {

    private final ImageService imageService;

    /**
     * 缓存时长：1年（秒）
     */
    private static final int CACHE_MAX_AGE = 31536000;

    /**
     * 转换HEIC图片为JPEG格式
     * 
     * <p>
     * 完整的处理流程：
     * <ol>
     * <li>接收图片URL参数</li>
     * <li>调用ImageService进行格式转换（自动缓存）</li>
     * <li>设置响应头（Content-Type、Cache-Control等）</li>
     * <li>返回转换后的JPEG图片二进制数据</li>
     * </ol>
     * 
     * <p>
     * 注意事项：
     * <ul>
     * <li>如果图片本身不是HEIC格式，会直接返回原图</li>
     * <li>转换结果会被缓存，相同URL的重复请求会直接从缓存返回</li>
     * <li>转换失败时返回500错误和错误信息</li>
     * </ul>
     * 
     * @param url      图片URL地址（支持HEIC及其他格式）
     * @param response HTTP响应对象
     */
    @GetMapping("/convert-heic")
    public void convertHEICToJpeg(@RequestParam String url, HttpServletResponse response) {
        try {
            log.info("收到图片转换请求: {}", url);

            // 调用Service层进行转换（自动缓存）
            byte[] imageData = imageService.convertImage(url);

            if (imageData == null || imageData.length == 0) {
                log.error("图片转换失败，返回数据为空: {}", url);
                writeError(response, "图片转换失败");
                return;
            }

            // 设置响应头
            response.setContentType("image/jpeg");
            response.setContentLength(imageData.length);
            response.setHeader("Cache-Control", "public, max-age=" + CACHE_MAX_AGE);

            // 写入图片数据
            response.getOutputStream().write(imageData);
            response.getOutputStream().flush();

            log.info("图片转换成功，大小: {} bytes", imageData.length);

        } catch (Exception e) {
            log.error("图片转换异常: {}", url, e);
            try {
                writeError(response, "图片转换失败: " + e.getMessage());
            } catch (IOException ioException) {
                log.error("写入错误响应失败", ioException);
            }
        }
    }

    /**
     * 写入错误响应
     * 
     * @param response HTTP响应对象
     * @param message  错误消息
     * @throws IOException IO异常
     */
    private void writeError(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.setContentType("text/plain;charset=UTF-8");
        response.getWriter().write(message);
    }
}
