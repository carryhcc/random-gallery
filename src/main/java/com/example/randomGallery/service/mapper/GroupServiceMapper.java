package com.example.randomGallery.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.example.randomGallery.entity.DO.GroupDO;
import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface GroupServiceMapper extends BaseMapper<GroupDO> {

    /**
     * 条件查询分组列表 (Unpaged)
     */
    List<GroupVO> selectGroupList(@Param("qry") GroupQry qry);

    /**
     * 条件查询分组列表 (Paged)
     */
    IPage<GroupVO> selectGroupPage(IPage<GroupVO> page, @Param("qry") GroupQry qry);
}
