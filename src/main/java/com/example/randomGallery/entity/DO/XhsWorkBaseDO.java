package com.example.randomGallery.entity.DO;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 作品基础信息DO（对应数据库表：xhs_work_base）
 */
@Data
@TableName(value = "xhs_work_base") // 指定关联的数据库表名
public class XhsWorkBaseDO {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO) // 主键自增策略，匹配数据库的AUTO_INCREMENT
    private Long id;

    /**
     * 响应消息（如：获取作品数据成功）
     */
    @TableField(value = "message")
    private String message;

    /**
     * 请求参数-作品链接
     */
    @TableField(value = "params_url")
    private String paramsUrl;

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
     * 收藏数量（如：10+）
     */
    @TableField(value = "collect_count")
    private String collectCount;

    /**
     * 评论数量（如：10+）
     */
    @TableField(value = "comment_count")
    private String commentCount;

    /**
     * 分享数量（如：10+）
     */
    @TableField(value = "share_count")
    private String shareCount;

    /**
     * 点赞数量（如：10+）
     */
    @TableField(value = "like_count")
    private String likeCount;

    /**
     * 作品标签（多个标签拼接）
     */
    @TableField(value = "work_tags")
    private String workTags;

    /**
     * 作品唯一ID（平台ID）
     */
    @TableField(value = "work_id")
    private String workId;

    /**
     * 作品链接
     */
    @TableField(value = "work_url")
    private String workUrl;

    /**
     * 作品标题
     */
    @TableField(value = "work_title")
    private String workTitle;

    /**
     * 作品描述（可能包含多个话题标签）
     */
    @TableField(value = "work_description")
    private String workDescription;

    /**
     * 作品类型（如：图文）
     */
    @TableField(value = "work_type")
    private String workType;

    /**
     * 发布时间（格式：2025-12-23_14:22:51）
     */
    @TableField(value = "publish_time")
    private String publishTime;

    /**
     * 最后更新时间
     */
    @TableField(value = "last_update_time")
    private String lastUpdateTime;

    /**
     * 时间戳（如：1766499771.0）
     */
    @TableField(value = "timestamp")
    private BigDecimal timestamp; // 对应数据库DECIMAL(20,1)，避免浮点精度丢失

    /**
     * 作者昵称
     */
    @TableField(value = "author_nickname")
    private String authorNickname;

    /**
     * 作者ID
     */
    @TableField(value = "author_id")
    private String authorId;

    /**
     * 作者主页链接
     */
    @TableField(value = "author_url")
    private String authorUrl;

    /**
     * 记录创建时间（数据库自动生成，插入时无需手动赋值）
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT) // 插入时自动填充
    private LocalDateTime createTime;

    /**
     * 是否删除
     */
     @TableField(value = "is_delete")
    private Boolean isDelete;
}
