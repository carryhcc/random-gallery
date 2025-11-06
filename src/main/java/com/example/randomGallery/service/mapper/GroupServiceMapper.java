package com.example.randomGallery.service.mapper;

import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupServiceMapper {

    /**
     * 根据分组ID查询分组信息
     * @param groupId
     * @param tableName
     * @return
     */
    GroupVO queryGroupById(@Param("groupId") Long groupId, @Param("tableName") String tableName);

    /**
     * 查询所有分组ID列表
     * @param tableName
     * @return
     */
    List<Long> selectGroupIdList(@Param("tableName") String tableName);

    /**
     * 条件查询分组列表
      * @param qry
     * @param tableName
     * @return
     */
    List<GroupVO> queryGroupList(@Param("qry") GroupQry qry, @Param("tableName") String tableName);


    void updateById(@Param("vo") GroupVO vo, @Param("tableName") String tableName);
}
