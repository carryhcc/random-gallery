package com.example.randomGallery.server.Impl;


import com.example.randomGallery.entity.DO.PicInfoDO;
import com.example.randomGallery.entity.VO.PicGroupVO;
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

    @Override
    public void getTheLimitValue() {
        Integer maxId = picServiceMapper.getMaxId();
        Integer minId = picServiceMapper.getMinId();
        Integer maxGroupId = picServiceMapper.getMaxGroupId();
        Integer minGroupId = picServiceMapper.getMinGroupId();
    }

    @Override
    public String getUrlById(Integer id) {
        return picServiceMapper.getUrlById(id);
    }

    @Override
    public PicGroupVO getRandomGroupPicList(Integer groupId) {
        PicGroupVO picGroupVO = new PicGroupVO();
        List<PicInfoDO> randomGroupPicList = picServiceMapper.getRandomGroupPicList(groupId);
        String groupName = randomGroupPicList.stream().map(PicInfoDO::getPicName).filter(Objects::nonNull).findFirst().orElse(null);
        picGroupVO.setGroupName(groupName);
        List<String> list = randomGroupPicList.stream().map(PicInfoDO::getPicUrl).toList();
        picGroupVO.setUrlList(list);
        return picGroupVO;
    }
}
