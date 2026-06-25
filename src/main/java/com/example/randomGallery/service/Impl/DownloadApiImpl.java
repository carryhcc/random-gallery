package com.example.randomGallery.service.Impl;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.example.randomGallery.entity.QO.DownLoadQry;
import com.example.randomGallery.service.DownloadApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadApiImpl implements DownloadApi {

    private final XhsDataSaveService xhsDataSaveService;

    @Value("${other.downloader.url}")
    private String xhsDetailUrl;

    @Qualifier("taskExecutor")
    private final Executor taskExecutor;

    @Override
    public void addDownloadTask(DownLoadQry qry) {
        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始下载任务，参数: {}", qry);
                String result = HttpUtil.post(xhsDetailUrl, JSONUtil.toJsonStr(qry));
                xhsDataSaveService.saveXhsData(result);
            } catch (Exception e) {
                log.error("下载任务异步处理异常: {}", qry.getUrl(), e);
            }
        }, taskExecutor);
    }
}
