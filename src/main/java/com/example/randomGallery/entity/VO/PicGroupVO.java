package com.example.randomGallery.entity.VO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 图片分组视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PicGroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 图片URL列表
     */
    private List<String> urlList;
}
