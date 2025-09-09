package com.example.randomGallery.entity.DO;

import lombok.Data;

import java.io.Serializable;

@Data
public class PicInfoDO implements Serializable {
    private Integer id;
    private String picUrl;
    private String picName;
    private Integer groupId;
    private Integer isDelete;
}
