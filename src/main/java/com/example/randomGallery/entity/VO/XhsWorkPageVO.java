package com.example.randomGallery.entity.VO;

import lombok.Data;

import java.util.List;

/**
 * 作品分页 VO
 */
@Data
public class XhsWorkPageVO {
    /**
     * 作品列表
     */
    private List<XhsWorkListVO> works;

    /**
     * 是否还有更多数据
     */
    private Boolean hasMore;
}
