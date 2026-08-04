package com.example.randomGallery.entity.VO;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 下载任务历史记录 VO
 */
@Data
public class XhsDownloadTaskVO {

    /**
     * 任务ID
     */
    private Long id;

    /**
     * 作品链接
     */
    private String url;

    /**
     * 任务状态（0=等待中，1=已完成，2=失败）
     */
    private Integer status;

    /**
     * 任务状态名称
     */
    private String statusName;

    /**
     * 解析后的作品ID（成功后有值，用于跳转详情页）
     */
    private String workId;

    /**
     * 解析后的作品标题
     */
    private String workTitle;

    /**
     * 解析后的作品链接
     */
    private String workUrl;

    /**
     * 失败原因
     */
    private String errorMessage;

    /**
     * 重试次数
     */
    private Integer retryCount;

    /**
     * 添加时间
     */
    private LocalDateTime createTime;

    /**
     * 完成/失败时间
     */
    private LocalDateTime finishTime;
}
