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

    private final PicServiceMapper picServiceMapper;
    private final CacheService cacheService;

    public PicServiceApiImpl(PicServiceMapper picServiceMapper, CacheService cacheService) {
        this.picServiceMapper = picServiceMapper;
        this.cacheService = cacheService;
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
