package com.example.randomGallery.entity.DO;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访问用户表DO
 */
@Data
@TableName(value = "visit_user")
public class VisitUserDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 唯一访客ID
     */
    private String uuid;

    /**
     * IP地址
     */
    private String ip;

    /**
     * 浏览器
     */
    private String browser;

    /**
     * 操作系统
     */
    private String os;

    /**
     * 设备类型
     */
    @TableField("device_type")
    private String deviceType;

    /**
     * User Agent
     */
    @TableField("user_agent")
    private String userAgent;

    /**
     * 地理位置
     */
    private String location;

    /**
     * 首次访问时间
     */
    @TableField("first_visit_time")
    private LocalDateTime firstVisitTime;

    /**
     * 最后访问时间
     */
    @TableField("last_visit_time")
    private LocalDateTime lastVisitTime;

    /**
     * 访问次数
     */
    @TableField("visit_count")
    private Integer visitCount;
}
