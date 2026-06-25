package com.example.randomGallery.controller;

import cn.hutool.core.collection.CollUtil;
import com.example.randomGallery.common.Result;
import com.example.randomGallery.entity.QO.PicQry;
import com.example.randomGallery.entity.VO.ImageData;
import com.example.randomGallery.entity.VO.PicVO;
import com.example.randomGallery.service.CacheService;
import com.example.randomGallery.service.PicServiceApi;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 图片API控制器 - 处理图片相关的API接口
 */
@Slf4j
@RestController
@RequestMapping("/api/pic")
@RequiredArgsConstructor
public class PicApiController {

    private final CacheService cacheService;

    private final PicServiceApi picServiceApi;

    private final RestTemplate restTemplate;

    private final ThreadPoolExecutor threadPoolExecutor;

    private static final int MAX_DOWNLOAD_LIMIT = 100;

    /**
     * 获取随机单张图片地址
     */
    @GetMapping("/random/one")
    public Result<PicVO> getRandomPic() {
        PicVO picVO = picServiceApi.getInfoById(cacheService.getRandomId());
        return Result.success("获取图片成功", picVO);
    }

    /**
     * 根据分组id查询图片列表
     */
    @PostMapping("/list")
    public Result<List<PicVO>> list(@RequestBody PicQry qry) {
        List<PicVO> list = picServiceApi.list(qry);
        return Result.success("获取图片成功", list);
    }

    /**
     * 批量下载分组图片（多线程处理）
     *
     * @param groupId  分组ID
     * @param response HttpServletResponse 对象，用于返回压缩包
     */
    @GetMapping("/download")
    public void downLoadGroup(@RequestParam Long groupId, HttpServletResponse response) {
        List<String> picUrlList = picServiceApi.downLoadGroup(groupId);
        if (CollUtil.isEmpty(picUrlList)) {
            writeText(response, "分组下没有图片");
            return;
        }

        // 超出上限时截断
        if (picUrlList.size() > MAX_DOWNLOAD_LIMIT) {
            log.warn("分组 {} 图片数量 {} 超出下载上限 {}，截断处理", groupId, picUrlList.size(), MAX_DOWNLOAD_LIMIT);
            picUrlList = picUrlList.subList(0, MAX_DOWNLOAD_LIMIT);
        }

        // 并发下载（保留索引保证顺序）
        List<CompletableFuture<ImageData>> futures = new ArrayList<>();
        for (int i = 0; i < picUrlList.size(); i++) {
            final int index = i;
            final String url = picUrlList.get(i);
            futures.add(CompletableFuture.supplyAsync(() -> {
                try {
                    byte[] bytes = restTemplate.getForObject(url, byte[].class);
                    return new ImageData(index, bytes);
                } catch (Exception e) {
                    log.error("下载失败: {}", url, e);
                    return new ImageData(index, null);
                }
            }, threadPoolExecutor));
        }

        // 设置响应头
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition", "attachment; filename=\"group_" + groupId + ".zip\"");

        // 等待所有下载完成后写 ZIP（峰值内存为 N 张图片字节之和，上限 MAX_DOWNLOAD_LIMIT 张）
        try (ZipOutputStream zipOut = new ZipOutputStream(response.getOutputStream())) {
            List<ImageData> images = futures.stream().map(CompletableFuture::join).toList();
            for (ImageData img : images) {
                if (img.getBytes() == null) continue;
                String ext = inferExtension(picUrlList.get(img.getIndex()));
                String fileName = "image_" + (img.getIndex() + 1) + ext;
                zipOut.putNextEntry(new ZipEntry(fileName));
                zipOut.write(img.getBytes());
                zipOut.closeEntry();
            }
            zipOut.finish();
        } catch (Exception e) {
            log.error("ZIP 打包失败 groupId={}", groupId, e);
            writeText(response, "下载失败");
        }
    }

    private String inferExtension(String url) {
        if (url == null) return ".jpg";
        String path = url.contains("?") ? url.substring(0, url.indexOf('?')) : url;
        int dot = path.lastIndexOf('.');
        if (dot > 0 && dot < path.length() - 1) {
            String ext = path.substring(dot).toLowerCase();
            if (ext.matches("\\.(jpg|jpeg|png|gif|webp|heic|avif)")) {
                return ext;
            }
        }
        return ".jpg";
    }

    private void writeText(HttpServletResponse response, String msg) {
        try {
            response.setContentType("text/plain;charset=UTF-8");
            response.getWriter().write(msg);
        } catch (Exception e) {
            log.error("写入响应失败: {}", msg, e);
        }
    }

}
