package com.example.randomGallery.service.mapper;


import com.example.randomGallery.entity.DO.PicDO;
import com.example.randomGallery.entity.VO.PicVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface PicServiceMapper {

    Integer getMaxId(@Param("tableName") String tableName);

    Integer getMinId(@Param("tableName") String tableName);

    Integer getMaxGroupId(@Param("tableName") String tableName);

    Integer getMinGroupId(@Param("tableName") String tableName);

    /**
     * 根据id获取图片信息
     * @param tableName
     * @param id
     * @return
     */
    PicVO getInfoById(@Param("tableName") String tableName, @Param("id") Integer id);

    /**
     * 获取分组总数
     * @param tableName
     * @param groupId
     * @return
     */
    Integer getGroupCount(@Param("tableName") String tableName, @Param("groupId") Integer groupId);

    PicVO getGroupRandomPicInfo(@Param("tableName") String tableName, @Param("groupId") Integer groupId);
}
