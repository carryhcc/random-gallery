package com.example.randomGallery.entity.DO;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 图片信息数据对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("pic_info")
public class PicDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId
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
    private Long groupId;

    /**
     * 是否删除 0-未删除 1-已删除
     */
    @TableLogic
    private Integer isDelete;
}
