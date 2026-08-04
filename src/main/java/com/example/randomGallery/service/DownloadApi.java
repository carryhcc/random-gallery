package com.example.randomGallery.service;

import com.example.randomGallery.entity.QO.DownLoadQry;
import org.springframework.stereotype.Service;

@Service
public interface DownloadApi {

    /**
     * 添加下载任务（落库历史记录并投递到消费队列）
     */
    void addDownloadTask(DownLoadQry qry);
}
