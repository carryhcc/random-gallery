package com.example.randomGallery.server;

import com.example.randomGallery.entity.VO.PicGroupVO;
import org.springframework.stereotype.Service;

@Service
public interface PicServiceApi {

    String getUrlById(Integer id);

    PicGroupVO getRandomGroupPicList(Integer groupId);
}
