package com.example.randomGallery.entity.VO;

import lombok.Data;

/**
 * 作品列表项 VO（用于瀑布流展示）
 */
@Data
public class XhsWorkListVO {
    /**
     * 数据库主键ID
     */
    private Long id;

    /**
     * 作品唯一ID（平台ID）
     */
    private String workId;

    /**
     * 作品标题
     */
    private String workTitle;

    /**
     * 作者昵称
     */
    private String authorNickname;

    /**
     * 发布时间
     */
    private String publishTime;

    /**
     * 封面图URL（第一张图片）
     */
    private String coverImageUrl;

    /**
     * 图片数量
     */
    private Integer imageCount;

    /**
     * 动图数量
     */
    private Integer gifCount;
}
