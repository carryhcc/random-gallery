package com.example.randomGallery.service.Impl;

import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupVO;
import com.example.randomGallery.entity.VO.PicVO;

import com.example.randomGallery.service.CacheService;
import com.example.randomGallery.service.GroupServiceApi;
import com.example.randomGallery.service.mapper.GroupServiceMapper;
import com.example.randomGallery.service.mapper.PicServiceMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 分组服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GroupServiceApiImpl implements GroupServiceApi {

    private final GroupServiceMapper groupServiceMapper;
    private final CacheService cacheService;
    private final PicServiceMapper picServiceMapper;

    @Override
    public GroupVO queryGroupById(Integer groupId) {
        String sqlName = cacheService.getGroupSqlName();
        return groupServiceMapper.queryGroupById(groupId, sqlName);
    }

    /**
     * 查询分组列表，支持分页
     *
     * @param qry 查询条件，包含分页参数
     * @return 分页后的分组列表
     */
    @Override
    public List<GroupVO> queryGroupList(GroupQry qry) {
        log.debug("查询分组列表，参数: {}", qry);
        String sqlName = cacheService.getGroupSqlName();
        PageHelper.startPage(qry.getPageIndex(), qry.getPageSize());
        List<GroupVO> result = groupServiceMapper.queryGroupList(qry, sqlName);

        log.debug("查询分组列表完成，返回 {} 条记录", result.size());
        return result;
    }

    @Override
    public void updateGroupInfo() {
        // 定时任务更新分组图片/总数信息
        String sqlName = cacheService.getGroupSqlName();
        List<Integer> groupIdList = groupServiceMapper.selectGroupIdList(sqlName);
        for (Integer groupId : groupIdList) {
            Integer count = picServiceMapper.getGroupCount(sqlName, groupId);
            PicVO picVO = picServiceMapper.getGroupRandomPicInfo(sqlName, groupId);
            GroupVO groupVO = new GroupVO();
            groupVO.setGroupId(groupId);
            groupVO.setGroupName(picVO.getPicName());
            groupVO.setGroupUrl(picVO.getPicUrl());
            groupVO.setGroupCount(count);
            groupServiceMapper.updateById(groupVO, sqlName);
        }
    }
}
