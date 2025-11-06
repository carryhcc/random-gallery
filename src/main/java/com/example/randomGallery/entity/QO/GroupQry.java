package com.example.randomGallery.entity.QO;

import com.example.randomGallery.entity.common.PageQuery;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分组查询对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupQry extends PageQuery implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 分组名称
     */
    private String groupName;

    /**
     * 分组ID
     */
    private Long groupId;

    /**
     * 分组ID
     */
    private List<Long> groupIdList;
}
