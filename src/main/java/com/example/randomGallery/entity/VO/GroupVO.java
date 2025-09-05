package com.example.randomGallery.entity.VO;

import lombok.Data;

import java.io.Serializable;

@Data
public class GroupVO implements Serializable {

    /**
     * 分组名称
     */
    private String  picName;

    /**
     * 分组id
     */
    private String  groupId;
}
