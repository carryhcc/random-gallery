package com.example.randomGallery.entity.DO;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作者信息DO（对应数据库表：author）
 */
@Data
@TableName(value = "author")
public class AuthorDO {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 作者ID（小红书平台ID）
     */
    @TableField(value = "author_id")
    private String authorId;

    /**
     * 作者昵称
     */
    @TableField(value = "author_nickname")
    private String authorNickname;

    /**
     * 作者主页链接
     */
    @TableField(value = "author_url")
    private String authorUrl;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
