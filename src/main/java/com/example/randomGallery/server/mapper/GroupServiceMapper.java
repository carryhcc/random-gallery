package com.example.randomGallery.server.mapper;

import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

@Mapper
public interface GroupServiceMapper {

    Integer queryGroupCount(@Param("qry") GroupQry qry, @Param("tableName") String tableName);

    List<GroupVO> queryGroupList(@Param("qry") GroupQry qry, @Param("tableName") String tableName, RowBounds rowBounds);
}
