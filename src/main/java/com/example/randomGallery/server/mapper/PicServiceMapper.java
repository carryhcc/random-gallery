package com.example.randomGallery.server.mapper;


import com.example.randomGallery.entity.DO.PicInfoDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface PicServiceMapper {

    Integer getMaxId(@Param("tableName") String tableName);

    Integer getMinId(@Param("tableName") String tableName);

    Integer getMaxGroupId(@Param("tableName") String tableName);

    Integer getMinGroupId(@Param("tableName") String tableName);

    String getUrlById(@Param("tableName") String tableName, @Param("id") Integer id);

    List<PicInfoDO> getRandomGroupPicList(@Param("tableName") String tableName, @Param("groupId") Integer groupId);
}
