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
     * 获取作品详情
     * 
     * @param workId 作品ID
     * @return 作品详情
     */
    XhsWorkDetailVO getXhsWorkDetail(String workId);
}
