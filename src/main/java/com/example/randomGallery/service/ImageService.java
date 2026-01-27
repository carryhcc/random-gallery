package com.example.randomGallery.service;

import cn.hutool.core.util.StrUtil;
import com.example.randomGallery.service.image.HeicConversionService;
import com.example.randomGallery.service.image.ImageDownloadService;
import com.example.randomGallery.service.image.ImageFormatDetector;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 图片处理门面服务
 * 
 * <p>
 * 作为门面（Facade）模式的实现，整合图片下载、格式检测、格式转换等功能，
 * 为上层控制器提供统一的服务接口。
 * 
 * <p>
 * 核心职責：
 * <ul>
 * <li>协调各个子服务完成完整的图片处理流程</li>
 * <li>提供向后兼容的API接口</li>
 * <li>统一的异常处理和日志记录</li>
 * </ul>
 * 
 * <p>
 * 依赖的子服务：
 * <ul>
 * <li>{@link ImageDownloadService} - 图片下载</li>
 * <li>{@link ImageFormatDetector} - 格式检测</li>
 * <li>{@link HeicConversionService} - HEIC转换</li>
 * </ul>
 * 
 * @author random-gallery
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ImageService {

    private final ImageDownloadService downloadService;
    private final ImageFormatDetector formatDetector;
    private final HeicConversionService conversionService;

    /**
     * 转换图片为浏览器兼容格式（核心方法）
     * 
     * <p>
     * 完整的处理流程：
     * <ol>
     * <li>检查缓存，如果命中直接返回</li>
     * <li>下载原始图片</li>
     * <li>检测是否为HEIC格式</li>
     * <li>如果是HEIC则转换为JPEG，否则直接返回原图</li>
     * <li>缓存转换结果</li>
     * </ol>
     * 
     * @param url 图片URL
     * @return 转换后的图片字节数组（HEIC会转为JPEG，其他格式原样返回），失败时返回null
     */
    @Cacheable(value = "heiCConvertCache", key = "#url", unless = "#result == null")
    public byte[] convertImage(String url) {
        if (StrUtil.isEmpty(url)) {
            log.warn("图片转换失败: URL为空");
            return null;
        }

        try {
            log.info("开始处理图片: {}", url);

            // 步骤1: 下载原始图片
            log.debug("下载原始图片");
            byte[] originalImage = downloadService.download(url);

            if (originalImage == null || originalImage.length == 0) {
                log.error("图片下载失败: {}", url);
                return null;
            }

            log.debug("图片下载成功，大小: {} bytes", originalImage.length);

            // 步骤2: 检测是否为HEIC格式
            log.debug("检测图片格式");
            boolean isHEIC = formatDetector.isHEICByBytes(originalImage);

            if (!isHEIC) {
                log.info("非HEIC格式，直接返回原图");
                return originalImage;
            }

            // 步骤3: 转换HEIC为JPEG
            log.debug("检测到HEIC格式，开始转换");
            byte[] convertedImage = conversionService.convert(originalImage);

            if (convertedImage == null || convertedImage.length == 0) {
                log.error("HEIC转换失败: {}", url);
                return null;
            }

            log.info("图片转换完成: {} -> JPEG", url);
            return convertedImage;

        } catch (Exception e) {
            log.error("图片处理异常: {}", url, e);
            throw new RuntimeException("图片处理失败: " + e.getMessage(), e);
        }
    }

    // ==================== 向后兼容的方法 ====================

    /**
     * 批量检测HEIC图片（预热缓存）
     * 
     * <p>
     * 委托给 {@link ImageFormatDetector#batchPreDetect(List)}
     * 
     * @param urls 图片URL列表
     */
    public void batchDetectHEIC(List<String> urls) {
        formatDetector.batchPreDetect(urls);
    }

    /**
     * 检测URL是否为HEIC格式图片（带缓存）
     * 
     * <p>
     * 委托给 {@link ImageFormatDetector#isHEICByContentType(String)}
     * 
     * @param url 图片URL
     * @return 是否为HEIC格式
     */
    public boolean isHEICImage(String url) {
        return formatDetector.isHEICByContentType(url);
    }

    /**
     * 检测字节数组是否为HEIC格式
     * 
     * <p>
     * 委托给 {@link ImageFormatDetector#isHEICByBytes(byte[])}
     * 
     * @param data 图片字节数组
     * @return 是否为HEIC格式
     */
    public boolean isHEICBytes(byte[] data) {
        return formatDetector.isHEICByBytes(data);
    }

    /**
     * 下载图片到内存
     * 
     * <p>
     * 委托给 {@link ImageDownloadService#download(String)}
     * 
     * @param url 图片URL
     * @return 图片字节数组
     */
    public byte[] downloadImage(String url) {
        return downloadService.download(url);
    }
}
