package com.example.randomGallery.service;


import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface GroupServiceApi {


    GroupVO queryGroupById(Integer groupId);

    List<GroupVO> queryGroupList(GroupQry qry);

    void updateGroupInfo();
}
