package com.example.randomGallery.entity.DO;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 分组信息对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("group_info")
public class GroupDO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * 分组ID
     */
    @TableId
    private Long groupId;

    /**
     * 分组URL
     */
    private String groupUrl;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 分组总数
     */
    private Integer groupCount;

    /**
     * 是否删除 0-未删除 1-已删除
     */
    @TableLogic
    private Integer isDelete;
}
