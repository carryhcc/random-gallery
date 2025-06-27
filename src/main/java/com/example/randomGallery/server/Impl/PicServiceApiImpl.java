package com.example.randomGallery.server.Impl;


import com.example.randomGallery.entity.DO.PicInfoDO;
import com.example.randomGallery.entity.VO.PicGroupVO;
import com.example.randomGallery.server.CacheService;
import com.example.randomGallery.server.PicServiceApi;
import com.example.randomGallery.server.mapper.PicServiceMapper;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class PicServiceApiImpl implements PicServiceApi {

    @Resource
    private PicServiceMapper picServiceMapper;

    @Resource
    private CacheService cacheService;

    @Override
    public void getTheLimitValue() {
        Integer maxId = picServiceMapper.getMaxId(cacheService.getSqlName());
        Integer minId = picServiceMapper.getMinId(cacheService.getSqlName());
        Integer maxGroupId = picServiceMapper.getMaxGroupId(cacheService.getSqlName());
        Integer minGroupId = picServiceMapper.getMinGroupId(cacheService.getSqlName());
    }

    @Override
    public String getUrlById(Integer id) {
        return picServiceMapper.getUrlById(cacheService.getSqlName(), id);
    }

    @Override
    public PicGroupVO getRandomGroupPicList(Integer groupId) {
        PicGroupVO picGroupVO = new PicGroupVO();
        List<PicInfoDO> randomGroupPicList = picServiceMapper.getRandomGroupPicList(cacheService.getSqlName(), groupId);
        String groupName = randomGroupPicList.stream().map(PicInfoDO::getPicName).filter(Objects::nonNull).findFirst().orElse(null);
        picGroupVO.setGroupName(groupName);
        List<String> list = randomGroupPicList.stream().map(PicInfoDO::getPicUrl).toList();
        picGroupVO.setUrlList(list);
        return picGroupVO;
    }
}
