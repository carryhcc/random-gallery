package com.example.randomGallery.entity.QO;

import lombok.Data;

import java.io.Serializable;

@Data
public class GroupQry implements Serializable {

    /**
     * 分组名称
     */
    private String picName;

    /**
     * 分组id
     */
    private String groupId;

    /**
     * 页码，默认1
     */
    private Integer pageIndex = 1;
    /**
     * 每页大小，默认10
     */
    private Integer pageSize = 10;
}
