package com.example.randomGallery.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.randomGallery.entity.DO.TagDO;
import com.example.randomGallery.entity.VO.TagVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 标签Mapper接口
 */
@Mapper
public interface TagMapper extends BaseMapper<TagDO> {

    List<TagVO> selectTagsWithWorkCount();

    List<TagVO> selectByKeyword(@Param("keyword") String keyword);
}
