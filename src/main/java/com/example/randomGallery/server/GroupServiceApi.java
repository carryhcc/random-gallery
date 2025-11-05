package com.example.randomGallery.server;


import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupVO;
import com.example.randomGallery.entity.VO.PageResult;
import com.example.randomGallery.entity.VO.RandomGalleryItemVO;
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

    /**
     * 随机画廊查询，按条件随机返回指定条数，每组一张随机图
     */
    List<RandomGalleryItemVO> queryRandomGallery(GroupQry qry, Integer limit);
}
