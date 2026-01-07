package com.example.randomGallery.service;

import com.example.randomGallery.entity.VO.AuthorVO;

import java.util.List;

/**
 * 作者服务接口
 */
public interface AuthorService {

    /**
     * 获取所有作者列表
     * 
     * @return 作者列表
     */
    List<AuthorVO> getAllAuthors();

    /**
     * 保存或更新作者信息
     * 
     * @param authorId       作者ID
     * @param authorNickname 作者昵称
     * @param authorUrl      作者主页链接
     */
    void saveOrUpdateAuthor(String authorId, String authorNickname, String authorUrl);

    /**
     * 创建作者作品关联
     * 
     * @param authorId 作者ID
     * @param workId   作品ID
     */
    void createAuthorWorkRelation(String authorId, String workId);
}
