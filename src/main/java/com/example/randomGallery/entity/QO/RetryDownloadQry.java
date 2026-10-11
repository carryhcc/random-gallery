package com.example.randomGallery.entity.QO;

import lombok.Data;

/** 重试下载任务参数 */
@Data
public class RetryDownloadQry {

    /** 本次重试使用的 Cookie；null 表示回退到下载器默认配置 */
    private String cookie;
}
