package com.example.randomGallery.entity.DO;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.randomGallery.entity.common.DownloadTaskStatusEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 下载任务历史记录 DO（对应数据库表：xhs_download_task）
 */
@Data
@TableName(value = "xhs_download_task")
public class XhsDownloadTaskDO {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 作品链接
     */
    @TableField(value = "url")
    private String url;

    /**
     * 请求参数-是否下载（0=否，1=是）
     */
    @TableField(value = "params_download")
    private Boolean paramsDownload;

    /**
     * 请求参数-索引
     */
    @TableField(value = "params_index")
    private String paramsIndex;

    /**
     * 请求参数-Cookie（可能较长）
     */
    @TableField(value = "params_cookie")
    private String paramsCookie;

    /**
     * 请求参数-代理地址
     */
    @TableField(value = "params_proxy")
    private String paramsProxy;

    /**
     * 请求参数-是否跳过（0=否，1=是）
     */
    @TableField(value = "params_skip")
    private Boolean paramsSkip;

    /**
     * 任务状态（0=等待中，1=已完成，2=失败）
     */
    @TableField(value = "status")
    private DownloadTaskStatusEnum status;

    /**
     * 解析后的作品ID
     */
    @TableField(value = "work_id")
    private String workId;

    /**
     * 解析后的作品标题
     */
    @TableField(value = "work_title")
    private String workTitle;

    /**
     * 解析后的作品链接
     */
    @TableField(value = "work_url")
    private String workUrl;

    /**
     * 失败原因
     */
    @TableField(value = "error_message")
    private String errorMessage;

    /**
     * 重试次数
     */
    @TableField(value = "retry_count")
    private Integer retryCount;

    /**
     * 添加时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 完成/失败时间
     */
    @TableField(value = "finish_time")
    private LocalDateTime finishTime;
}
