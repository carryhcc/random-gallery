package com.example.randomGallery.server;

import com.example.randomGallery.entity.VO.PicGroupVO;
import org.springframework.stereotype.Service;

@Service
public interface PicServiceApi {


    /**
     * 获取图片,套图的最大值和最下值
     */
    void getTheLimitValue();

    String getUrlById(Integer id);

    PicGroupVO getRandomGroupPicList(Integer groupId);
}
