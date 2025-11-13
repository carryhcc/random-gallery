package com.example.randomGallery.entity.VO;

import lombok.Data;

import java.util.List;

@Data
public class GroupPageVO {
    private List<GroupVO> images; // 图片列表
    private boolean hasMore;

    public GroupPageVO(List<GroupVO> images, boolean hasMore) {
        this.images = images;
        this.hasMore = hasMore;
    }
}
