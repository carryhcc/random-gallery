package com.example.randomGallery.server;


import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupVO;
import com.example.randomGallery.entity.VO.PageResult;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface GroupServiceApi {

    List<GroupVO> queryGroupList(GroupQry qry);

    Integer queryGroupCount(GroupQry qry);
    
    /**
     * 分页查询分组列表，返回分页信息
     * @param qry 查询条件
     * @return 分页结果
     */
    PageResult<GroupVO> queryGroupListWithPage(GroupQry qry);
}
