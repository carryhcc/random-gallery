package com.example.randomGallery.service;

import com.example.randomGallery.entity.QO.PicQry;
import com.example.randomGallery.entity.VO.PicVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface PicServiceApi {

    PicVO getInfoById(Long id);

    List<PicVO> list(PicQry qry);
}
