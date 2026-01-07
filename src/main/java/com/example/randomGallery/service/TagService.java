package com.example.randomGallery.service;

import com.example.randomGallery.entity.VO.TagVO;

import java.util.List;

/**
 * 标签服务接口
 */
public interface TagService {

    /**
     * 获取所有标签列表
     * 
     * @return 标签列表
     */
    List<TagVO> getAllTags();

    /**
     * 根据标签名获取或创建标签
     * 
     * @param tagName 标签名称
     * @return 标签ID
     */
    Long getOrCreateTag(String tagName);

    /**
     * 创建标签作品关联
     * 
     * @param tagId  标签ID
     * @param workId 作品ID
     */
    void createTagWorkRelation(Long tagId, String workId);

    /**
     * 处理作品标签（解析work_tags字段并创建关联）
     * 
     * @param workTags 作品标签字符串（空格分隔）
     * @param workId   作品ID
     */
    void processWorkTags(String workTags, String workId);
}
