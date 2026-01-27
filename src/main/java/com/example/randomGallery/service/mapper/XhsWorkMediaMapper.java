package com.example.randomGallery.service.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.randomGallery.entity.DO.XhsWorkMediaDO;
import com.example.randomGallery.entity.VO.RandomGifVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * 作品媒体地址Mapper
 */
@Mapper
public interface XhsWorkMediaMapper extends BaseMapper<XhsWorkMediaDO> {

    /**
     * 查询随机GIF
     */
    RandomGifVO randomGifInfo();

    /**
     * 根据ID 获取GIF信息
     */
    RandomGifVO getGifById(@Param("id") Long id);
}