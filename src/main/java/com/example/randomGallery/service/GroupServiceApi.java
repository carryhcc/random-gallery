package com.example.randomGallery.service;


import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupPageVO;
import com.example.randomGallery.entity.VO.GroupVO;
import com.example.randomGallery.entity.common.PageResult;
import org.springframework.stereotype.Service;

@Service
public interface GroupServiceApi {


    GroupVO queryGroupById(Long groupId);

    PageResult<GroupVO> queryGroupList(GroupQry qry);

    void updateGroupInfo();

    GroupPageVO loadMore(int page);
}
