package com.example.randomGallery.controller;

import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupVO;
import com.example.randomGallery.server.GroupServiceApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;


/**
 * 外部接口提供
 */

@Controller
public class OutController {

    @Resource
    private GroupServiceApi groupServiceApi;

    @ResponseBody
    @PostMapping("/queryGroupList")
    public List<GroupVO> queryGroupList(@RequestBody GroupQry qry) {
        List<GroupVO> groupVOS = groupServiceApi.queryGroupList(qry);
        return groupVOS;
    }
}
