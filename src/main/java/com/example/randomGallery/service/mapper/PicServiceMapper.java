package com.example.randomGallery.service.mapper;


import com.example.randomGallery.entity.DO.PicDO;
import com.example.randomGallery.entity.QO.PicQry;
import com.example.randomGallery.entity.VO.GroupVO;
import com.example.randomGallery.entity.VO.PicVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface PicServiceMapper {

    Long getMaxId(@Param("tableName") String tableName);

    Long getMinId(@Param("tableName") String tableName);

    Long getMaxGroupId(@Param("tableName") String tableName);

    Long getMinGroupId(@Param("tableName") String tableName);

    /**
     * 根据id获取图片信息
     * @param tableName
     * @param id
     * @return
     */
    PicVO getInfoById(@Param("tableName") String tableName, @Param("id") Long id);

    /**
     * 查询图片列表
     * @param tableName
     * @param qry
     * @return
     */
    List<PicVO> list(@Param("tableName") String tableName, @Param("qry") PicQry qry);

    GroupVO queryPicCountInfo(@Param("tableName") String tableName, @Param("groupId") Long groupId);
}
