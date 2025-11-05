package com.example.randomGallery.server.Impl;

import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupVO;
import com.example.randomGallery.entity.VO.PageResult;
import com.example.randomGallery.entity.VO.RandomGalleryItemVO;
import com.example.randomGallery.server.CacheService;
import com.example.randomGallery.server.GroupServiceApi;
import com.example.randomGallery.server.mapper.GroupServiceMapper;
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

    /**
     * 查询分组列表，支持分页
     *
     * @param qry 查询条件，包含分页参数
     * @return 分页后的分组列表
     */
    @Override
    public List<GroupVO> queryGroupList(GroupQry qry) {
        log.debug("查询分组列表，参数: {}", qry);
        String sqlName = cacheService.getSqlName();
        
        // 使用PageHelper进行真分页
        PageHelper.startPage(qry.getPageIndex(), qry.getPageSize());
        List<GroupVO> result = groupServiceMapper.queryGroupList(qry, sqlName);
        
        log.debug("查询分组列表完成，返回 {} 条记录", result.size());
        return result;
    }

    @Override
    public Integer queryGroupCount(GroupQry qry) {
        log.debug("查询分组总数，参数: {}", qry);
        String sqlName = cacheService.getSqlName();
        Integer count = groupServiceMapper.queryGroupCount(qry, sqlName);
        
        log.debug("查询分组总数完成，总数: {}", count);
        return count;
    }
    
    @Override
    public PageResult<GroupVO> queryGroupListWithPage(GroupQry qry) {
        log.debug("分页查询分组列表，参数: {}", qry);
        String sqlName = cacheService.getSqlName();
        
        // 使用PageHelper进行真分页
        PageHelper.startPage(qry.getPageIndex(), qry.getPageSize());
        List<GroupVO> list = groupServiceMapper.queryGroupList(qry, sqlName);
        
        // 获取分页信息
        PageInfo<GroupVO> pageInfo = new PageInfo<>(list);
        
        // 构建分页结果
        PageResult<GroupVO> result = new PageResult<>(
            pageInfo.getList(),
            pageInfo.getTotal(),
            pageInfo.getPageNum(),
            pageInfo.getPageSize()
        );
        
        log.debug("分页查询分组列表完成，返回 {} 条记录，共 {} 页", 
            result.getList().size(), result.getPages());
        return result;
    }

    @Override
    public List<RandomGalleryItemVO> queryRandomGallery(GroupQry qry, Integer limit) {
        log.debug("随机画廊查询，参数: {}，limit: {}", qry, limit);
        String sqlName = cacheService.getSqlName();
        int finalLimit = (limit == null || limit <= 0) ? 10 : limit;
        List<RandomGalleryItemVO> list = groupServiceMapper.queryRandomGallery(qry, sqlName, finalLimit);
        log.debug("随机画廊查询完成，返回 {} 条记录", list.size());
        return list;
    }
}
