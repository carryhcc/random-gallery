package com.example.randomGallery.entity.VO;

import lombok.Data;

/**
 * 随机GIF VO
 */
@Data
public class RandomGifVO {

    /**
     * 媒体ID
     */
    private Long id;

    /**
     * 媒体地址
     */
    private String mediaUrl;

    /**
     * 作品唯一ID（平台ID）
     */
    private String workId;

    /**
     * 关联基础表的主键ID
     */
    private Long workBaseId;
}
