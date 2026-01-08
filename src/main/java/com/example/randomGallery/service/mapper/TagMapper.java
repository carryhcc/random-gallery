package com.example.randomGallery.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.randomGallery.entity.DO.TagDO;
import com.example.randomGallery.entity.VO.TagVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 标签Mapper接口
 */
@Mapper
public interface TagMapper extends BaseMapper<TagDO> {

    @Select("SELECT t.id, t.tag_name as tagName, COUNT(tw.work_id) as workCount " +
            "FROM tags t " +
            "LEFT JOIN tag_work tw ON t.id = tw.tag_id " +
            "GROUP BY t.id, t.tag_name")
    List<TagVO> selectTagsWithWorkCount();
}
