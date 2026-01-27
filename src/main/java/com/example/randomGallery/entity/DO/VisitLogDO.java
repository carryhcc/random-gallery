package com.example.randomGallery.entity.DO;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访问日志表DO
 */
@Data
@TableName(value = "visit_log")
public class VisitLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的访问用户ID
     */
    @TableField("visit_user_id")
    private Long visitUserId;

    /**
     * 请求URI
     */
    private String uri;

    /**
     * HTTP方法
     */
    private String method;

    /**
     * 请求参数
     */
    private String params;

    /**
     * 响应状态码
     */
    private Integer status;

    /**
     * 耗时(ms)
     */
    private Long duration;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
