package com.example.randomGallery.entity.DO;

import com.baomidou.mybatisplus.annotation.*;
import com.example.randomGallery.entity.common.MediaTypeEnum;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作品媒体地址DO（对应数据库表：xhs_work_media）
 */
@Data
@TableName(value = "xhs_work_media") // 指定关联的数据库表名
public class XhsWorkMediaDO {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联基础表的主键ID
     */
    @TableField(value = "work_base_id")
    private Long workBaseId;

    /**
     * 作品唯一ID（平台ID，用于关联和去重）
     */
    @TableField(value = "work_id")
    private String workId;

    /**
     * 媒体类型（image=图片地址，gif=动图/视频地址）
     */
    @TableField(value = "media_type")
    private MediaTypeEnum mediaType; // 使用枚举类，替代String，类型更安全

    /**
     * 媒体地址（图片/动图URL）
     */
    @TableField(value = "media_url")
    private String mediaUrl;

    /**
     * 媒体地址在原始列表中的排序索引（保留顺序）
     */
    @TableField(value = "sort_index")
    private Integer sortIndex;

    /**
     * 记录创建时间（数据库自动生成，插入时无需手动赋值）
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}