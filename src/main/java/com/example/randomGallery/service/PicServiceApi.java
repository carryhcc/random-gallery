package com.example.randomGallery.service;

import com.example.randomGallery.entity.VO.GroupVO;
import com.example.randomGallery.entity.VO.PicVO;
import org.springframework.stereotype.Service;

@Service
public interface PicServiceApi {

    PicVO getInfoById(Integer id);
}
