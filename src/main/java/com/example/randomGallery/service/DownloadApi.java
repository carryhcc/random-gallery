package com.example.randomGallery.service;

import com.example.randomGallery.entity.QO.DownLoadQry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public interface DownloadApi {

    /**
     * 添加下载任务
     */
    @Transactional(rollbackFor = Exception.class)
    void addDownloadTask(DownLoadQry qry);
}
