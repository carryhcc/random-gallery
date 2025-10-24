package com.example.randomGallery.controller;

import com.example.randomGallery.common.Result;
import com.example.randomGallery.entity.VO.PicGroupVO;
import com.example.randomGallery.server.CacheService;
import com.example.randomGallery.server.PicServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    /**
     * 获取随机单张图片地址
     */
    @GetMapping("/random")
    public Result<String> getRandomPic() {
        log.debug("获取随机单张图片");
        String url = picServiceApi.getUrlById(cacheService.getRandomId());
        return Result.success("获取图片成功", url);
    }

    /**
     * 获取随机套图
     */
    @GetMapping("/group")
    public Result<Map<String, String>> getRandomGroup(@RequestParam(value = "groupId", required = false) Integer groupId) {
        log.debug("获取随机套图，groupId: {}", groupId);
        cacheService.resetTimer();
        Integer finalGroupId = Optional.ofNullable(groupId).orElseGet(cacheService::getRandomGroupId);
        PicGroupVO randomGroupPicList = picServiceApi.getRandomGroupPicList(finalGroupId);
        
        if (randomGroupPicList == null) {
            return Result.success("获取套图成功", Collections.emptyMap());
        }
        
        Map<String, String> result = Map.of(
            randomGroupPicList.getGroupName(), 
            randomGroupPicList.getUrlList().toString()
        );
        return Result.success("获取套图成功", result);
    }

    /**
     * 分页获取套图（流式加载）
     */
    @GetMapping("/group/paged")
    public Result<Map<String, Object>> getRandomGroupPaged(
            @RequestParam(value = "groupId", required = false) Integer groupId,
            @RequestParam(value = "page", defaultValue = "1") Integer page,
            @RequestParam(value = "size", defaultValue = "6") Integer size) {
        log.debug("分页获取套图，groupId: {}, page: {}, size: {}", groupId, page, size);
        try {
            cacheService.resetTimer();
            Integer finalGroupId = Optional.ofNullable(groupId).orElseGet(cacheService::getRandomGroupId);
            PicGroupVO randomGroupPicList = picServiceApi.getRandomGroupPicList(finalGroupId);
            
            if (randomGroupPicList == null || randomGroupPicList.getUrlList() == null || randomGroupPicList.getUrlList().isEmpty()) {
                Map<String, Object> result = Map.of(
                    "groupName", "无数据",
                    "images", Collections.emptyList(),
                    "currentPage", page,
                    "totalPages", 0,
                    "hasMore", false,
                    "totalImages", 0
                );
                return Result.success("获取套图成功", result);
            }
            
            List<String> allImages = randomGroupPicList.getUrlList();
            int totalImages = allImages.size();
            int totalPages = (int) Math.ceil((double) totalImages / size);
            
            // 计算当前页的图片范围
            int startIndex = (page - 1) * size;
            int endIndex = Math.min(startIndex + size, totalImages);
            
            // 确保索引有效
            if (startIndex >= totalImages) {
                startIndex = 0;
                endIndex = 0;
            }
            
            List<String> pageImages;
            if (startIndex >= endIndex) {
                pageImages = Collections.emptyList();
            } else {
                pageImages = allImages.subList(startIndex, endIndex);
            }
            
            boolean hasMore = page < totalPages;
            
            Map<String, Object> result = Map.of(
                "groupName", randomGroupPicList.getGroupName(),
                "images", pageImages,
                "currentPage", page,
                "totalPages", totalPages,
                "hasMore", hasMore,
                "totalImages", totalImages
            );
            
            return Result.success("获取套图成功", result);
        } catch (Exception e) {
            log.error("分页获取套图失败", e);
            Map<String, Object> result = Map.of(
                "groupName", "错误",
                "images", Collections.emptyList(),
                "currentPage", page,
                "totalPages", 0,
                "hasMore", false,
                "totalImages", 0
            );
            return Result.error(500, "获取套图失败: " + e.getMessage());
        }
    }
}
