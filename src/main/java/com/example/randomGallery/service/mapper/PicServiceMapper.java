package com.example.randomGallery.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.randomGallery.entity.DO.PicDO;
import com.example.randomGallery.entity.QO.PicQry;
import com.example.randomGallery.entity.VO.PicVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PicServiceMapper extends BaseMapper<PicDO> {

    /**
     * 查询图片列表 (Paged)
     */
    IPage<PicVO> selectPicPage(IPage<PicVO> page, @Param("qry") PicQry qry);

    /**
     * 统计有效图片总数。用于随机取图，替代以往把全量 ID 拉进内存的做法
     */
    Long selectValidPicCount();

    /**
     * 取第一条 id >= 给定值的有效图片 ID（走主键索引，实测 0.03~0.11ms）
     */
    Long selectFirstValidIdSince(@Param("id") long id);
}
