package com.example.randomGallery.service;

import com.example.randomGallery.entity.VO.XhsWorkDetailVO;
import com.example.randomGallery.entity.VO.XhsWorkPageVO;
import org.springframework.stereotype.Service;

/**
 * 作品查询服务
 */
@Service
public interface XhsWorkService {

    /**
     * 分页查询作品列表
     * 
     * @param page     页码（从0开始）
     * @param pageSize 每页数量
     * @return 作品分页数据
     */
    XhsWorkPageVO pageXhsWorks(int page, int pageSize);

    /**
     * 分页查询作品列表（支持筛选）
     * 
     * @param page     页码（从0开始）
     * @param pageSize 每页数量
     * @param authorId 作者ID（可选）
     * @param tagId    标签ID（可选）
     * @return 作品分页数据
     */
    XhsWorkPageVO pageXhsWorksWithFilter(int page, int pageSize, String authorId, Long tagId);

    /**
     * 获取作品详情
     * 
     * @param workId 作品ID
     * @return 作品详情
     */
    XhsWorkDetailVO getXhsWorkDetail(String workId);

    /**
     * 删除作品（软删除）
     * 
     * @param workId 作品ID
     */
    void deleteWork(String workId);

    /**
     * 删除媒体（软删除）
     * 
     * @param mediaUrl 媒体URL (或者根据ID删除，Plan说是根据MediaID，这里确认用MediaID更安全)
     */
    void deleteMedia(Long id);
}
