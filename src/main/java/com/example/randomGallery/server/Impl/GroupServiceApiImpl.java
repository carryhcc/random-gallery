package com.example.randomGallery.server.Impl;

import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupVO;
import com.example.randomGallery.server.CacheService;
import com.example.randomGallery.server.GroupServiceApi;
import com.example.randomGallery.server.mapper.GroupServiceMapper;
import jakarta.annotation.Resource;
import org.apache.ibatis.session.RowBounds;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GroupServiceApiImpl implements GroupServiceApi {


    @Resource
    private GroupServiceMapper groupServiceMapper;

    @Resource
    private CacheService cacheService;

    /**
     * 查询分组列表，支持分页
     *
     * @param qry 查询条件，包含分页参数
     * @return 分页后的分组列表
     */
    @Override
    public List<GroupVO> queryGroupList(GroupQry qry) {
        String sqlName = cacheService.getSqlName();
        // 内存分页
        RowBounds rowBounds = new RowBounds(qry.getPageIndex(), qry.getPageSize());
        return groupServiceMapper.queryGroupList(qry, sqlName, rowBounds);
    }
}
