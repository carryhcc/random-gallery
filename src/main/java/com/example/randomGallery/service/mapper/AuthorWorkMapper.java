package com.example.randomGallery.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.randomGallery.entity.DO.AuthorWorkDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作者作品关联Mapper接口
 */
@Mapper
public interface AuthorWorkMapper extends BaseMapper<AuthorWorkDO> {
}
