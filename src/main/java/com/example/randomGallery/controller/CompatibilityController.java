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
 * 兼容性控制器 - 处理旧版本API接口，保持向后兼容
 */
@Slf4j
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class CompatibilityController {

    private final GroupServiceApi groupServiceApi;

    /**
     * 查询分组列表（兼容旧接口）
     * @deprecated 建议使用 /api/group/list
     */
    @Deprecated
    @PostMapping("/queryGroupList")
    public Result<List<GroupVO>> queryGroupList(@RequestBody GroupQry qry) {
        log.info("查询分组列表（兼容接口），参数: {}", qry);
        List<GroupVO> result = groupServiceApi.queryGroupList(qry);
        return Result.success("查询成功", result);
    }

    /**
     * 查询分组总数（兼容旧接口）
     * @deprecated 建议使用 /api/group/count
     */
    @Deprecated
    @PostMapping("/queryGroupCount")
    public Result<Integer> queryGroupCount(@RequestBody GroupQry qry) {
        log.info("查询分组总数（兼容接口），参数: {}", qry);
        Integer count = groupServiceApi.queryGroupCount(qry);
        return Result.success("查询成功", count);
    }
    
    /**
     * 分页查询分组列表，返回分页信息（兼容旧接口）
     * @deprecated 建议使用 /api/group/list/paged
     */
    @Deprecated
    @PostMapping("/queryGroupListWithPage")
    public Result<PageResult<GroupVO>> queryGroupListWithPage(@RequestBody GroupQry qry) {
        log.info("分页查询分组列表（兼容接口），参数: {}", qry);
        PageResult<GroupVO> result = groupServiceApi.queryGroupListWithPage(qry);
        return Result.success("查询成功", result);
    }
}
