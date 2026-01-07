package com.example.randomGallery.entity.VO;

import lombok.Data;

/**
 * 作者VO（用于前端展示）
 */
@Data
public class AuthorVO {

    /**
     * 作者ID
     */
    private String authorId;

    /**
     * 作者昵称
     */
    private String authorNickname;

    /**
     * 作者主页链接
     */
    private String authorUrl;

    /**
     * 作品数量
     */
    private Long workCount;
}
