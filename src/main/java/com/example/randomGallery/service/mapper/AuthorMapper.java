package com.example.randomGallery.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.randomGallery.entity.DO.AuthorDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 作者Mapper接口
 */
@Mapper
public interface AuthorMapper extends BaseMapper<AuthorDO> {
}
