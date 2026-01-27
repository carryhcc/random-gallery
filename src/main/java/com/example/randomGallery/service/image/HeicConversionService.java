package com.example.randomGallery.service.image;

import cn.hutool.core.util.StrUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

/**
 * HEIC图片转换服务
 *
 * <p>
 * 负责将HEIC/HEIF格式的图片转换为JPEG格式。
 * 使用 h2non/imaginary 服务进行实际的格式转换。
 *
 * <p>
 * 转换配置：
 * <ul>
 * <li>目标格式: JPEG</li>
 * <li>图片质量: 90</li>
 * </ul>
 *
 * @author random-gallery
 * @see <a href="https://github.com/h2non/imaginary">h2non/imaginary</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HeicConversionService {

    private final RestTemplate restTemplate;

    @Value("${other.imaginary.url}")
    private String url;

    /**
     * 将HEIC图片转换为JPEG格式
     *
     * <p>
     * 通过POST请求将HEIC图片数据发送给imaginary服务，
     * 服务会返回转换后的JPEG格式图片。
     *
     * @param heicData HEIC格式的图片字节数组
     * @return 转换后的JPEG图片字节数组，转换失败时返回null
     * @throws RuntimeException 转换过程中发生异常时抛出
     */
    public byte[] convert(byte[] heicData) {
        if (heicData == null || heicData.length == 0) {
            log.warn("HEIC转换失败: 输入数据为空");
            return null;
        }

        try {
            String imaginaryUrl = url;
            if (StrUtil.isEmpty(imaginaryUrl)) {
                log.error("imaginary 服务URL未配置");
                return null;
            }
            log.debug("准备转换HEIC图片，大小: {} bytes, imaginary URL: {}", heicData.length, imaginaryUrl);

            // 构建multipart请求体
            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();

            // 使用ByteArrayResource并指定文件名
            ByteArrayResource resource = new ByteArrayResource(heicData) {
                @Override
                public String getFilename() {
                    return "image.heic";
                }
            };
            body.add("file", resource);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            log.debug("发送转换请求到imaginary服务");

            // 发送POST请求
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    imaginaryUrl,
                    HttpMethod.POST,
                    requestEntity,
                    byte[].class);

            // 检查转换结果
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                byte[] jpegData = response.getBody();
                log.info("HEIC转换成功，原始大小: {} bytes, 转换后: {} bytes",
                        heicData.length, jpegData.length);
                return jpegData;
            } else {
                log.error("HEIC转换失败，HTTP状态码: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            log.error("HEIC转换异常", e);
            throw new RuntimeException("HEIC转换失败: " + e.getMessage(), e);
        }
    }
}
