package com.example.randomGallery.entity.QO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 分组查询对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupQry implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分组名称
     */
    private String picName;

    /**
     * 分组ID
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
