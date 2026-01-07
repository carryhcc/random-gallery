package com.example.randomGallery.controller;

import com.example.randomGallery.common.Result;
import com.example.randomGallery.entity.VO.AuthorVO;
import com.example.randomGallery.entity.VO.TagVO;
import com.example.randomGallery.entity.VO.XhsWorkDetailVO;
import com.example.randomGallery.entity.VO.XhsWorkPageVO;
import com.example.randomGallery.service.AuthorService;
import com.example.randomGallery.service.DataMigrationService;
import com.example.randomGallery.service.TagService;
import com.example.randomGallery.service.XhsWorkService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 作品查询控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/xhsWork")
@RequiredArgsConstructor
public class XhsWorkApiController {

    private final XhsWorkService xhsWorkService;
    private final AuthorService authorService;
    private final TagService tagService;
    private final DataMigrationService dataMigrationService;

    /**
     * 执行历史数据迁移
     */
    @PostMapping("/migrate")
    public Result<String> migrateData() {
        log.info("开始执行历史数据迁移");
        try {
            dataMigrationService.migrateHistoricalTags();
            String info = dataMigrationService.getMigrationInfo();
            return Result.success(info);
        } catch (Exception e) {
            log.error("数据迁移失败", e);
            return Result.error("迁移失败: " + e.getMessage());
        }
    }

    /**
     * 分页查询作品列表（支持筛选）
     */
    @GetMapping("/list")
    public Result<XhsWorkPageVO> listWorks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) String authorId,
            @RequestParam(required = false) Long tagId) {
        log.info("查询作品列表，page={}, authorId={}, tagId={}", page, authorId, tagId);
        XhsWorkPageVO result = xhsWorkService.pageXhsWorksWithFilter(page, 3, authorId, tagId);
        return Result.success(result);
    }

    /**
     * 获取所有作者列表
     */
    @GetMapping("/authors")
    public Result<List<AuthorVO>> getAuthors() {
        log.info("获取作者列表");
        List<AuthorVO> authors = authorService.getAllAuthors();
        return Result.success(authors);
    }

    /**
     * 获取所有标签列表
     */
    @GetMapping("/tags")
    public Result<List<TagVO>> getTags() {
        log.info("获取标签列表");
        List<TagVO> tags = tagService.getAllTags();
        return Result.success(tags);
    }

    /**
     * 查询作品详情
     */
    @GetMapping("/detail/{workId}")
    public Result<XhsWorkDetailVO> getWorkDetail(@PathVariable String workId) {
        log.info("查询作品详情，workId={}", workId);
        XhsWorkDetailVO detail = xhsWorkService.getXhsWorkDetail(workId);
        if (detail == null) {
            return Result.error("作品不存在");
        }
        return Result.success(detail);
    }

    /**
     * 删除作品
     */
    @PostMapping("/delete/{workId}")
    public Result<String> deleteWork(@PathVariable String workId) {
        xhsWorkService.deleteWork(workId);
        return Result.success("删除成功");
    }

    /**
     * 删除媒体
     */
    @PostMapping("/media/delete/{id}")
    public Result<String> deleteMedia(@PathVariable Long id) {
        xhsWorkService.deleteMedia(id);
        return Result.success("删除成功");
    }
}
