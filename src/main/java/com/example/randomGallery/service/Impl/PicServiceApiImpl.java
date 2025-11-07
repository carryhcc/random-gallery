package com.example.randomGallery.service.Impl;

import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.QO.PicQry;
import com.example.randomGallery.entity.VO.GroupVO;
import com.example.randomGallery.entity.VO.PicVO;
import com.example.randomGallery.service.CacheService;
import com.example.randomGallery.service.PicServiceApi;
import com.example.randomGallery.service.mapper.PicServiceMapper;

import com.github.pagehelper.PageHelper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;


/**
 * 图片服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PicServiceApiImpl implements PicServiceApi {

    private final PicServiceMapper picServiceMapper;

    private final CacheService cacheService;

    @Override
    public PicVO getInfoById(Long id) {
        return picServiceMapper.getInfoById(cacheService.getPicSqlName(), id);
    }

    @Override
    public List<PicVO> list(PicQry qry) {
        String sqlName = cacheService.getGroupSqlName();
        PageHelper.startPage(qry.getPageIndex(), qry.getPageSize());
        return picServiceMapper.list(sqlName, qry);
    }
}
