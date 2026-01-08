package com.example.randomGallery.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.randomGallery.entity.DO.GroupDO;
import com.example.randomGallery.entity.DO.PicDO;
import com.example.randomGallery.entity.QO.PicQry;
import com.example.randomGallery.entity.VO.PicVO;
import com.example.randomGallery.service.PicServiceApi;
import com.example.randomGallery.service.mapper.PicServiceMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 图片服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PicServiceApiImpl implements PicServiceApi {

    private final PicServiceMapper picServiceMapper;

    @Override
    public PicVO getInfoById(Long id) {
        PicDO picDO = picServiceMapper.selectById(id);
        ObjectUtil.defaultIfNull(picDO, new PicDO());
        return BeanUtil.copyProperties(picDO, PicVO.class);
    }

    @Override
    public List<PicVO> list(PicQry qry) {
        log.debug("查询图片列表，参数: {}", qry);
        // 使用 MyBatis-Plus 分页
        Page<PicVO> page = new Page<>(qry.getPageIndex(), qry.getPageSize());
        picServiceMapper.selectPicPage(page, qry);
        return page.getRecords();
    }

    @Override
    public List<String> downLoadGroup(Long groupId) {
        // 批量查询图片信息（主键IN查询，性能极高）
        PicQry picQry = new PicQry();
        picQry.setGroupId(groupId);
        List<PicVO> imageList = picServiceMapper.selectPicPage(new Page<>(1, 1000), picQry).getRecords();
        // 获取所有图片地址
        return imageList.stream().map(PicVO::getPicUrl).collect(Collectors.toList());
    }
}
