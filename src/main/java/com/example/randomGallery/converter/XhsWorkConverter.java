package com.example.randomGallery.converter;

import com.example.randomGallery.entity.DO.XhsWorkBaseDO;
import com.example.randomGallery.entity.VO.XhsWorkListVO;
import org.springframework.stereotype.Component;

/**
 * 作品DO-VO转换工具类
 */
@Component
public class XhsWorkConverter {

    /**
     * 将XhsWorkBaseDO转换为XhsWorkListVO
     */
    public XhsWorkListVO toListVO(XhsWorkBaseDO baseDO) {
        if (baseDO == null) {
            return null;
        }

        XhsWorkListVO vo = new XhsWorkListVO();
        vo.setId(baseDO.getId());
        vo.setWorkId(baseDO.getWorkId());
        vo.setWorkTitle(baseDO.getWorkTitle());
        vo.setAuthorNickname(baseDO.getAuthorNickname());
        vo.setPublishTime(baseDO.getPublishTime());

        return vo;
    }
}
