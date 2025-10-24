package com.example.randomGallery.entity.DO;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;

/**
 * 图片信息数据对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PicInfoDO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 主键ID
     */
    private Integer id;
    
    /**
     * 图片URL
     */
    private String picUrl;
    
    /**
     * 图片名称
     */
    private String picName;
    
    /**
     * 分组ID
     */
    private Integer groupId;
    
    /**
     * 是否删除 0-未删除 1-已删除
     */
    private Integer isDelete;
}
