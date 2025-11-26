package com.example.randomGallery.entity.VO;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class PicCount implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;
    /**
     * 环境名称
     */
    private String env;
    /**
     * 分组数量
     */
    private Long groupCount;
    /**
     * 图片数量
     */
    private Long picCount;
}
