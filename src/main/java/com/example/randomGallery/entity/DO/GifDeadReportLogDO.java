package com.example.randomGallery.entity.DO;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * GIF 死链上报日志（对应数据库表：gif_dead_report_log）
 */
@Data
@TableName(value = "gif_dead_report_log")
public class GifDeadReportLogDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 上报的媒体ID */
    @TableField(value = "media_id")
    private Long mediaId;

    /** 上报来源：web / android */
    @TableField(value = "report_source")
    private String reportSource;

    /** 触发上报的客户端 IP */
    @TableField(value = "client_ip")
    private String clientIp;

    /** 记录创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
