package com.example.randomGallery.service.Impl;

import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupPageVO;
import com.example.randomGallery.entity.VO.GroupVO;
import com.example.randomGallery.entity.common.PageResult;

import com.example.randomGallery.service.CacheService;
import com.example.randomGallery.service.GroupServiceApi;
import com.example.randomGallery.service.mapper.GroupServiceMapper;
import com.example.randomGallery.service.mapper.PicServiceMapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    public GroupVO queryGroupById(Long groupId) {
        String sqlName = cacheService.getGroupSqlName();
        return groupServiceMapper.queryGroupById(groupId, sqlName);
    }

    /**
     * 查询分组列表，支持分页
     *
     * @param qry 查询条件，包含分页参数
     * @return 分页后的分组列表，包含分页信息
     */
    @Override
    public PageResult<GroupVO> queryGroupList(GroupQry qry) {
        log.debug("查询分组列表，参数: {}", qry);
        String sqlName = cacheService.getGroupSqlName();
        PageHelper.startPage(qry.getPageIndex(), qry.getPageSize());
        List<GroupVO> result = groupServiceMapper.queryGroupList(qry, sqlName);
        PageInfo<GroupVO> pageInfo = new PageInfo<>(result);

        log.debug("查询分组列表完成，返回 {} 条记录，共 {} 页", result.size(), pageInfo.getPages());
        return new PageResult<>(
                pageInfo.getList(),
                pageInfo.getTotal(),
                pageInfo.getPageNum(),
                pageInfo.getPageSize()
        );
    }

    @Override
    public void updateGroupInfo() {
        // 定时任务更新分组图片/总数信息
        String sqlGroupName = cacheService.getGroupSqlName();
        String sqlPicName = cacheService.getPicSqlName();
        List<Long> groupIdList = groupServiceMapper.selectGroupIdList(sqlGroupName);
        for (Long groupId : groupIdList) {
            GroupVO groupVO = picServiceMapper.queryPicCountInfo(sqlPicName, groupId);
            groupServiceMapper.updateById(groupVO, sqlGroupName);
            log.info("定时任务更新分组图片/总数信息: {}", groupVO);
        }
    }

    /**
     * 加载更多图片（核心接口，单用户无会话隔离）
     *
     * @param page 当前页码（从0开始）
     * @return 图片分页数据（含是否有更多）
     */
    // 每页加载数量（固定9张）
    private static final int PAGE_SIZE = 9;

    @Override
    public GroupPageVO loadMore(int page) {
        // 默认进入时候刷新顺序
        if (page == 0) cacheService.buildGroupIDList();
        Long totalImageCount = cacheService.getTotalGroupCount();
        // 无图片数据时直接返回
        if (totalImageCount == 0) {
            return new GroupPageVO(Collections.emptyList(), false);
        }
        List<Long> shuffledSeq = cacheService.getShuffledSeq();
        // 计算当前页的ID起始和结束索引
        int offset = page * PAGE_SIZE;
        if (offset >= shuffledSeq.size()) {
            // 超出序列长度，无更多数据
            return new GroupPageVO(Collections.emptyList(), false);
        }
        int end = Math.min(offset + PAGE_SIZE, shuffledSeq.size());
        List<Long> currentPageIds = shuffledSeq.subList(offset, end);
        // 批量查询图片信息（主键IN查询，性能极高）
        GroupQry groupQry = new GroupQry();
        groupQry.setGroupIdList(currentPageIds);
        String sqlName = cacheService.getGroupSqlName();
        List<GroupVO> imageList = groupServiceMapper.queryGroupList(groupQry, sqlName);
        // 转换为前端需要的VO
        List<GroupVO> imageVOList = imageList.stream()
                .map(img -> new GroupVO(img.getGroupId(), img.getGroupUrl(), img.getGroupName(), img.getGroupCount()))
                .collect(Collectors.toList());
        // 判断是否还有更多数据（下一页是否超出总数量）
        boolean hasMore = (long) (page + 1) * PAGE_SIZE < totalImageCount;
        return new GroupPageVO(imageVOList, hasMore);
    }
}
