package com.example.randomGallery.entity.VO;

import lombok.Data;

/**
 * 标签VO（用于前端展示）
 */
@Data
public class TagVO {

    /**
     * 标签ID
     */
    private Long id;

    /**
     * 标签名称
     */
    private String tagName;

    /**
     * 作品数量
     */
    private Long workCount;
}
