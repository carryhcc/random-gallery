package com.example.randomGallery.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.randomGallery.entity.DO.TagWorkDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 标签作品关联Mapper接口
 */
@Mapper
public interface TagWorkMapper extends BaseMapper<TagWorkDO> {
}
