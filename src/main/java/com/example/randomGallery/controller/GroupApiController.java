package com.example.randomGallery.controller;

import com.example.randomGallery.common.Result;
import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupVO;
import com.example.randomGallery.entity.VO.PageResult;
import com.example.randomGallery.server.GroupServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 分组查询API控制器 - 处理分组相关的查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class GroupApiController {

    private final GroupServiceApi groupServiceApi;

    /**
     * 查询分组列表
     */
    @PostMapping("/list")
    public Result<List<GroupVO>> queryGroupList(@RequestBody GroupQry qry) {
        log.info("查询分组列表，参数: {}", qry);
        List<GroupVO> result = groupServiceApi.queryGroupList(qry);
        return Result.success("查询成功", result);
    }

    /**
     * 查询分组总数
     */
    @PostMapping("/count")
    public Result<Integer> queryGroupCount(@RequestBody GroupQry qry) {
        log.info("查询分组总数，参数: {}", qry);
        Integer count = groupServiceApi.queryGroupCount(qry);
        return Result.success("查询成功", count);
    }
    
    /**
     * 分页查询分组列表，返回分页信息（推荐使用）
     */
    @PostMapping("/list/paged")
    public Result<PageResult<GroupVO>> queryGroupListWithPage(@RequestBody GroupQry qry) {
        log.info("分页查询分组列表，参数: {}", qry);
        PageResult<GroupVO> result = groupServiceApi.queryGroupListWithPage(qry);
        return Result.success("查询成功", result);
    }
}
