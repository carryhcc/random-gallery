package com.example.randomGallery.service;

import com.example.randomGallery.entity.QO.PicQry;
import com.example.randomGallery.entity.VO.PicVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface PicServiceApi {

    /**
     * 根据ID查询图片信息
     *
     * @param id 图片ID
     * @return 图片VO
     */
    PicVO getInfoById(Long id);

    /**
     * 查询图片列表
     *
     * @param qry 查询参数
     * @return 图片VO列表
     */
    List<PicVO> list(PicQry qry);


    /**
     * 批量下载分组图片
     *
     * @param groupId 分组ID
     * @return 图片URL列表
     */
    List<String> downLoadGroup(Long groupId);
}
