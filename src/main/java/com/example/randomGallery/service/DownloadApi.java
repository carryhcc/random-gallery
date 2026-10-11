package com.example.randomGallery.service;

import com.example.randomGallery.common.Result;
import com.example.randomGallery.entity.QO.DownLoadQry;
import org.springframework.stereotype.Service;

@Service
public interface DownloadApi {

    /**
     * 添加下载任务（落库历史记录并投递到消费队列）
     */
    void addDownloadTask(DownLoadQry qry);

    /**
     * 校验小红书 Cookie 是否有效（仅元数据探测，不落库、不进消费队列）
     */
    Result<Boolean> validateCookie(String cookie);
}
