package com.example.randomGallery.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.randomGallery.entity.DO.PicDO;
import com.example.randomGallery.entity.QO.PicQry;
import com.example.randomGallery.entity.VO.GroupVO;
import com.example.randomGallery.entity.VO.PicVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface PicServiceMapper extends BaseMapper<PicDO> {

    /**
     * 查询图片列表 (Paged)
     */
    IPage<PicVO> selectPicPage(IPage<PicVO> page, @Param("qry") PicQry qry);

    GroupVO queryPicCountInfo(@Param("groupId") Long groupId);

    /**
     * 查询所有有效图片 ID（用于随机取图）
     */
    List<Long> selectAllValidPicIds();
}
