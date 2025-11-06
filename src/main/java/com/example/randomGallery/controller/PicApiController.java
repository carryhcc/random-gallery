package com.example.randomGallery.controller;

import com.example.randomGallery.common.Result;
import com.example.randomGallery.entity.VO.PicVO;
import com.example.randomGallery.service.CacheService;
import com.example.randomGallery.service.PicServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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
    @GetMapping("/random/one")
    public Result<PicVO> getRandomPic() {
        PicVO picVO = picServiceApi.getInfoById(cacheService.getRandomId());
        return Result.success("获取图片成功", picVO);
    }
}
