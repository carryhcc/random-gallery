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
     * 分组名称
     */
    private String picName;

    /**
     * 分组ID
     */
    private String groupId;
}
