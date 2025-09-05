package com.example.randomGallery.server;


import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupVO;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface GroupServiceApi {

    List<GroupVO> queryGroupList(GroupQry qry);

    Integer queryGroupCount(GroupQry qry);
}
