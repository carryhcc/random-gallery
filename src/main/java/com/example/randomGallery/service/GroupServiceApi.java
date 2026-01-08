package com.example.randomGallery.service;

import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupPageVO;
import com.example.randomGallery.entity.VO.GroupVO;
import com.example.randomGallery.entity.common.PageResult;
import org.springframework.stereotype.Service;

@Service
public interface GroupServiceApi {

    /**
     * 查询分组详情
     *
     * @param groupId 分组ID
     * @return 分组详情
     */
    GroupVO queryGroupById(Long groupId);

    /**
     * 查询分组列表
     *
     * @param qry 查询参数
     * @return 分组列表分页结果
     */
    PageResult<GroupVO> queryGroupList(GroupQry qry);

    /**
     * 更新分组信息
     */
    void updateGroupInfo();

    /**
     * 前端加载更多图片接口（单用户无需会话ID）
     *
     * @param page 当前页码（默认0）
     * @return 统一响应结果
     */
    GroupPageVO loadMore(int page, boolean refresh);
}
