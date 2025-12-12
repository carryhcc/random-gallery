package com.example.randomGallery.entity.VO;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分组视图对象
 */
@Data
@NoArgsConstructor
public class GroupVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    private Long groupId;

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

    public GroupVO(Long groupId, String groupName, String groupUrl, Integer groupCount) {
        this.groupId = groupId;
        this.groupName = groupName;
        this.groupUrl = groupUrl;
        this.groupCount = groupCount;
    }
}
