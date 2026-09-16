package com.example.randomGallery.entity.DO;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户收藏（对应数据库表：user_favorite）
 */
@Data
@TableName(value = "user_favorite")
public class UserFavoriteDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 媒体ID（对应 xhs_work_media.id） */
    @TableField(value = "media_id")
    private Long mediaId;

    /** 媒体类型：gif / pic */
    @TableField(value = "media_type")
    private String mediaType;

    /** 收藏时间 */
    @TableField(value = "created_at", fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
