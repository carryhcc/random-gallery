package com.example.randomGallery.controller;

import com.example.randomGallery.common.Result;
import com.example.randomGallery.entity.QO.GroupQry;
import com.example.randomGallery.entity.VO.GroupPageVO;
import com.example.randomGallery.entity.VO.GroupVO;
import com.example.randomGallery.entity.common.PageResult;
import com.example.randomGallery.service.CacheService;
import com.example.randomGallery.service.GroupServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

/**
 * 分组查询API控制器 - 处理分组相关的查询接口
 */
@Slf4j
@RestController
@RequestMapping("/api/group")
@RequiredArgsConstructor
public class GroupApiController {

    private final GroupServiceApi groupServiceApi;

    private final CacheService cacheService;

    /**
     * 获取随机分组信息
     */
    @GetMapping("/randomGroupInfo")
    public Result<GroupVO> getRandomGroupInfo(@RequestParam(value = "groupId", required = false) Long groupId) {
        groupId = groupId != null ? groupId : cacheService.getRandomGroupId();
        GroupVO groupInfo = groupServiceApi.queryGroupById(groupId);
        return Result.success("获取随机分组信息成功", groupInfo);
    }

    /**
     * 查询分组列表
     */
    @PostMapping("/list")
    public Result<PageResult<GroupVO>> queryGroupList(@RequestBody GroupQry qry) {
        log.info("查询分组列表，参数: {}", qry);
        PageResult<GroupVO> result = groupServiceApi.queryGroupList(qry);
        return Result.success("查询成功", result);
    }

    /**
     * 前端加载更多图片接口（单用户无需会话ID）
     *
     * @param page 当前页码（默认0）
     * @return 统一响应结果
     */
    @GetMapping("/loadMore")
    public Result<GroupPageVO> loadMore(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "false") boolean refresh) {
        GroupPageVO data = groupServiceApi.loadMore(page, refresh);
        return Result.success(data);
    }
}
