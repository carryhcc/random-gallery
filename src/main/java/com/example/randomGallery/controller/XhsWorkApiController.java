package com.example.randomGallery.controller;

import com.example.randomGallery.common.Result;
import com.example.randomGallery.entity.VO.XhsWorkDetailVO;
import com.example.randomGallery.entity.VO.XhsWorkPageVO;
import com.example.randomGallery.service.XhsWorkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 作品查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/xhsWork")
@RequiredArgsConstructor
public class XhsWorkApiController {

    private final XhsWorkService xhsWorkService;

    /**
     * 分页查询作品列表
     */
    @GetMapping("/list")
    public Result<XhsWorkPageVO> listWorks(@RequestParam(defaultValue = "0") int page) {
        log.info("查询作品列表，page={}", page);
        XhsWorkPageVO result = xhsWorkService.pageXhsWorks(page, 24);
        return Result.success(result);
    }

    /**
     * 查询作品详情
     */
    @GetMapping("/detail/{workId}")
    public Result<XhsWorkDetailVO> getWorkDetail(@PathVariable String workId) {
        log.info("查询作品详情，workId={}", workId);
        XhsWorkDetailVO detail = xhsWorkService.getXhsWorkDetail(workId);
        if (detail == null) {
            return Result.error("作品不存在");
        }
        return Result.success(detail);
    }
}
