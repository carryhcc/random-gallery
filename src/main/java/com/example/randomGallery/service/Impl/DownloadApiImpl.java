package com.example.randomGallery.service.Impl;

import com.example.randomGallery.entity.QO.DownLoadQry;
import com.example.randomGallery.service.DownloadApi;
import com.example.randomGallery.service.DownloadTaskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadApiImpl implements DownloadApi {

    private final DownloadTaskService downloadTaskService;
    private final DownloadTaskConsumer downloadTaskConsumer;

    @Override
    public void addDownloadTask(DownLoadQry qry) {
        // 1. 落库历史记录（状态=等待中）
        Long taskId = downloadTaskService.addTask(qry);
        // 2. 投递到单线程消费队列（消费时串行执行并加入 3~5s 随机延迟）
        downloadTaskConsumer.submit(taskId);
        log.info("下载任务已提交消费队列, taskId: {}, url: {}", taskId, qry.getUrl());
    }
}
