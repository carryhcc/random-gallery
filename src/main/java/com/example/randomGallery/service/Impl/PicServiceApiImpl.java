package com.example.randomGallery.service.Impl;

import com.example.randomGallery.entity.QO.PicQry;
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
        log.debug("查询图片列表，参数: {}", qry);
        String sqlName = cacheService.getPicSqlName();
        try (var ignored = PageHelper.startPage(qry.getPageIndex(), qry.getPageSize())) {
            return picServiceMapper.list(sqlName, qry);
        }
    }


    @Override
    public List<String> downLoadGroup(Long groupId) {
        // 批量查询图片信息（主键IN查询，性能极高）
        PicQry picQry = new PicQry();
        picQry.setGroupId(groupId);
        String sqlName = cacheService.getPicSqlName();
        List<PicVO> imageList = picServiceMapper.list(sqlName, picQry);
        // 获取所有图片地址
        return imageList.stream().map(PicVO::getPicUrl).toList();
    }
}
