package com.example.randomGallery.entity.VO;

import com.example.randomGallery.entity.DO.XhsWorkBaseDO;
import com.example.randomGallery.entity.DO.XhsWorkMediaDO;
import lombok.Data;

import java.util.List;

/**
 * 作品详情 VO
 */
@Data
public class XhsWorkDetailVO {
    /**
     * 作品基础信息
     */
    private XhsWorkBaseDO baseInfo;

    /**
     * 所有图片列表
     */
    private List<XhsWorkMediaDO> images;

    /**
     * 所有动图列表
     */
    private List<XhsWorkMediaDO> gifs;
}
