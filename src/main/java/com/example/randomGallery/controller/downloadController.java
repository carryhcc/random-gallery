package com.example.randomGallery.controller;

import com.example.randomGallery.common.Result;
import com.example.randomGallery.entity.QO.DownLoadQry;
import com.example.randomGallery.service.DownloadApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 图片下载处理
 */
@Slf4j
@RestController
@RequestMapping("/api/download")
@RequiredArgsConstructor
public class DownloadController {

    private final DownloadApi downloadApi;

    /**
     * 下载图片
     */
    @PostMapping("/xhs")
    public Result<String> download(@RequestBody DownLoadQry qry) {
        downloadApi.addDownloadTask(qry);
        return Result.success("下载任务添加成功");
    }
}
