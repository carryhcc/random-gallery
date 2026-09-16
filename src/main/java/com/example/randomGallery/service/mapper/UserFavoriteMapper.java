package com.example.randomGallery.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.randomGallery.entity.DO.UserFavoriteDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 用户收藏 Mapper
 */
@Mapper
public interface UserFavoriteMapper extends BaseMapper<UserFavoriteDO> {

    @Select("SELECT COUNT(*) FROM user_favorite WHERE media_id = #{mediaId} AND media_type = #{mediaType}")
    int countByMedia(@Param("mediaId") Long mediaId, @Param("mediaType") String mediaType);
}
