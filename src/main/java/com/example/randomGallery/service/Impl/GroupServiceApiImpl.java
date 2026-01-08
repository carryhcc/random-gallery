package com.example.randomGallery.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.randomGallery.entity.DO.GroupDO;
import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupPageVO;
import com.example.randomGallery.entity.VO.GroupVO;
import com.example.randomGallery.entity.common.PageResult;

import com.example.randomGallery.service.CacheService;
import com.example.randomGallery.service.GroupServiceApi;
import com.example.randomGallery.service.mapper.GroupServiceMapper;
import com.example.randomGallery.service.mapper.PicServiceMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
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
        GroupDO groupDO = groupServiceMapper.selectById(groupId);
        if (groupDO == null) {
            return null;
        }
        return BeanUtil.copyProperties(groupDO, GroupVO.class);
    }

    @Override
    public PageResult<GroupVO> queryGroupList(GroupQry qry) {
        log.debug("查询分组列表，参数: {}", qry);
        // 使用 MyBatis-Plus 分页
        Page<GroupVO> page = new Page<>(qry.getPageIndex(), qry.getPageSize());
        // 执行查询，结果会自动填充到 page 对象
        groupServiceMapper.selectGroupPage(page, qry);

        log.debug("查询分组列表完成，返回 {} 条记录，共 {} 页", page.getRecords().size(), page.getPages());
        return new PageResult<>(
                page.getRecords(),
                page.getTotal(),
                (int) page.getCurrent(),
                (int) page.getSize());
    }

    @Override
    public void updateGroupInfo() {
        // 定时任务更新分组图片/总数信息
        List<Object> objs = groupServiceMapper.selectObjs(new QueryWrapper<GroupDO>().select("group_id"));
        if (objs == null)
            return;

        List<Long> groupIdList = objs.stream()
                .map(obj -> Long.valueOf(obj.toString()))
                .toList();

        for (Long groupId : groupIdList) {
            GroupVO groupVO = picServiceMapper.queryPicCountInfo(groupId);
            if (groupVO != null) {
                GroupDO groupDO = BeanUtil.copyProperties(groupVO, GroupDO.class);
                groupServiceMapper.updateById(groupDO);
                log.info("定时任务更新分组图片/总数信息: {}", groupVO);
            }
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
        if (page == 0)
            cacheService.buildGroupIDList();
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
        List<GroupVO> imageList = groupServiceMapper.selectGroupList(groupQry);
        // 转换为前端需要的VO
        List<GroupVO> imageVOList = imageList.stream()
                .map(img -> new GroupVO(img.getGroupId(), img.getGroupName(), img.getGroupUrl(), img.getGroupCount()))
                .collect(Collectors.toList());
        // 判断是否还有更多数据（下一页是否超出总数量）
        boolean hasMore = (long) (page + 1) * PAGE_SIZE < totalImageCount;
        return new GroupPageVO(imageVOList, hasMore);
    }
}
