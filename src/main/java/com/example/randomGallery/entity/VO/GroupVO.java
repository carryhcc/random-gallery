package com.example.randomGallery.entity.VO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 分组视图对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    private Integer groupId;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 分组URL
     */
    private String groupUrl;

    /**
     * 分组总数
     */
    private Integer groupCount;
}
