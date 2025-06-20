package com.example.randomGallery.server.mapper;


import com.example.randomGallery.entity.DO.PicInfoDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


@Mapper
public interface PicServiceMapper {

    Integer getMaxId();

    Integer getMinId();

    Integer getMaxGroupId();

    Integer getMinGroupId();

    String getUrlById(@Param("id") Integer id);


    List<PicInfoDO> getRandomGroupPicList(@Param("groupId") Integer groupId);
}
