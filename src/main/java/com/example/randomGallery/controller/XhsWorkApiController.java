package com.example.randomGallery.controller;

import cn.hutool.core.util.ObjectUtil;
import com.example.randomGallery.common.Result;
import com.example.randomGallery.entity.QO.DownLoadQry;
import com.example.randomGallery.entity.VO.AuthorVO;
import com.example.randomGallery.entity.VO.RandomGifVO;
import com.example.randomGallery.entity.VO.TagVO;
import com.example.randomGallery.entity.VO.XhsDownloadTaskVO;
import com.example.randomGallery.entity.VO.XhsWorkDetailVO;
import com.example.randomGallery.entity.VO.XhsWorkPageVO;
import com.example.randomGallery.entity.common.PageResult;
import com.example.randomGallery.service.Impl.DownloadTaskConsumer;
import com.example.randomGallery.entity.DO.GifDeadReportLogDO;
import com.example.randomGallery.service.*;
import com.example.randomGallery.service.mapper.GifDeadReportLogMapper;
import com.example.randomGallery.service.mapper.XhsWorkMediaMapper;
import com.example.randomGallery.exception.NotFoundException;
import com.example.randomGallery.utils.UserAgentUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    private final DownloadApi downloadApi;
    private final DownloadTaskService downloadTaskService;
    private final DownloadTaskConsumer downloadTaskConsumer;
    private final GifDeadReportLogMapper gifDeadReportLogMapper;
    private final XhsWorkMediaMapper workMediaMapper;

    /**
     * 下载图片
     */
    @PostMapping("/download")
    public Result<String> download(@RequestBody DownLoadQry qry) {
        downloadApi.addDownloadTask(qry);
        return Result.success("下载任务添加成功");
    }

    /**
     * 分页查询下载任务历史记录（按添加时间倒序，支持状态筛选）
     */
    @GetMapping("/download/history")
    public Result<PageResult<XhsDownloadTaskVO>> downloadHistory(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Integer status) {
        PageResult<XhsDownloadTaskVO> result = downloadTaskService.pageHistory(page, size, status);
        return Result.success(result);
    }

    /**
     * 获取下载任务统计（进行中/等待中、已完成、失败数量）
     */
    @GetMapping("/download/stats")
    public Result<com.example.randomGallery.entity.VO.DownloadTaskStatsVO> downloadStats() {
        return Result.success(downloadTaskService.getStats());
    }

    /**
     * 删除指定的下载任务记录
     */
    @DeleteMapping("/download/task/{id}")
    public Result<String> deleteDownloadTask(@PathVariable Long id) {
        downloadTaskService.deleteTask(id);
        return Result.success("删除成功");
    }

    /**
     * 重试失败的下载任务
     */
    @PostMapping("/download/retry/{id}")
    public Result<String> retryDownload(@PathVariable Long id) {
        downloadTaskService.retryTask(id);
        downloadTaskConsumer.submit(id);
        return Result.success("重试任务已提交");
    }

    /**
     * 分页查询作品列表（支持筛选）
     */
    @GetMapping("/list")
    public Result<XhsWorkPageVO> listWorks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size,
            @RequestParam(required = false) String authorId,
            @RequestParam(required = false) Long tagId,
            @RequestParam(required = false) String str,
            @RequestParam(required = false) Integer seed,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "Sec-CH-UA", required = false) String secChUa,
            @RequestHeader(value = "X-Client-Native", required = false) String clientNative) {
        boolean skipHeicConversion = UserAgentUtils.isSafari(userAgent, secChUa)
                || "android".equalsIgnoreCase(clientNative);
        XhsWorkPageVO result = xhsWorkService.pageXhsWorksWithFilter(page, size, authorId, tagId, str, seed, skipHeicConversion);
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
     * 获取标签列表（支持 limit 参数限制返回数量）
     */
    @GetMapping("/tags")
    public Result<List<TagVO>> getTags(@RequestParam(required = false) Integer limit) {
        List<TagVO> tags = tagService.getAllTags();
        if (limit != null && limit > 0 && limit < tags.size()) {
            tags = tags.subList(0, limit);
        }
        return Result.success(tags);
    }

    /**
     * 根据关键词搜索标签
     */
    @GetMapping("/tags/search")
    public Result<List<TagVO>> searchTags(@RequestParam String q) {
        List<TagVO> tags = tagService.searchTags(q);
        return Result.success(tags);
    }

    /**
     * 查询作品详情
     */
    @GetMapping("/detail/{workId}")
    public Result<XhsWorkDetailVO> getWorkDetail(
            @PathVariable String workId,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            @RequestHeader(value = "Sec-CH-UA", required = false) String secChUa,
            @RequestHeader(value = "X-Client-Native", required = false) String clientNative) {
        boolean skipHeicConversion = UserAgentUtils.isSafari(userAgent, secChUa)
                || "android".equalsIgnoreCase(clientNative);
        XhsWorkDetailVO detail = xhsWorkService.getXhsWorkDetail(workId, skipHeicConversion);
        if (ObjectUtil.isNull(detail)) {
            throw new NotFoundException("作品不存在");
        }
        return Result.success(detail);
    }

    /**
     * 删除作品
     */
    @DeleteMapping("/{workId}")
    public Result<String> deleteWork(@PathVariable String workId) {
        xhsWorkService.deleteWork(workId);
        return Result.success("删除成功");
    }

    /**
     * 删除媒体
     */
    @DeleteMapping("/media/{id}")
    public Result<String> deleteMedia(@PathVariable Long id) {
        xhsWorkService.deleteMedia(id);
        return Result.success("删除成功");
    }

    /**
     * 获取随机GIF（支持排除已看过的ID）
     */
    @GetMapping("/randomGif")
    public Result<RandomGifVO> getRandomGif(
            @RequestParam(required = false) String exclude) {
        log.info("获取随机GIF, exclude={}", exclude);
        List<Long> excludeIds = parseExcludeIds(exclude);
        RandomGifVO randomGif = xhsWorkService.getRandomGif(excludeIds);
        if (randomGif == null) {
            return Result.error("暂无可用的GIF");
        }
        return Result.success(randomGif);
    }

    /**
     * 解析 exclude 参数（逗号分隔的ID列表）
     */
    private List<Long> parseExcludeIds(String exclude) {
        if (exclude == null || exclude.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return java.util.Arrays.stream(exclude.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            log.warn("exclude 参数解析失败: {}", exclude);
            return Collections.emptyList();
        }
    }

    /**
     * 上报动图外链失效（幂等：重复标记无副作用）
     */
    @GetMapping("/reportDead")
    public Result<String> reportDead(@RequestParam Long id, HttpServletRequest request) {
        xhsWorkService.reportDead(id);

        // 记录上报日志（异步容错，不影响主流程）
        try {
            GifDeadReportLogDO logDO = new GifDeadReportLogDO();
            logDO.setMediaId(id);
            logDO.setReportSource(getClientSource(request));
            logDO.setClientIp(getClientIp(request));
            gifDeadReportLogMapper.insert(logDO);
        } catch (Exception e) {
            log.warn("死链上报日志记录失败: mediaId={}", id, e);
        }

        return Result.success("已记录失效资源");
    }

    /**
     * 获取死链上报统计
     */
    @GetMapping("/deadReport/stats")
    public Result<Map<String, Object>> getDeadReportStats() {
        Map<String, Object> stats = gifDeadReportLogMapper.getReportStats();
        return Result.success(stats);
    }

    /**
     * 获取死链标记的 GIF 数量
     */
    @GetMapping("/deadReport/count")
    public Result<Long> getDeadCount() {
        Long count = workMediaMapper.selectCount(
                com.baomidou.mybatisplus.core.toolkit.Wrappers
                        .<com.example.randomGallery.entity.DO.XhsWorkMediaDO>lambdaQuery()
                        .eq(com.example.randomGallery.entity.DO.XhsWorkMediaDO::getIsDead, true));
        return Result.success(count);
    }

    // ── 收藏功能 ──────────────────────────────────────────

    /**
     * 切换收藏状态（已收藏→取消，未收藏→添加）
     */
    @GetMapping("/favorite/toggle")
    public Result<Boolean> toggleFavorite(
            @RequestParam Long id,
            @RequestParam(defaultValue = "gif") String type) {
        boolean isFav = xhsWorkService.toggleFavorite(id, type);
        return Result.success(isFav);
    }

    /**
     * 查询是否已收藏
     */
    @GetMapping("/favorite/check")
    public Result<Boolean> checkFavorite(
            @RequestParam Long id,
            @RequestParam(defaultValue = "gif") String type) {
        return Result.success(xhsWorkService.isFavorite(id, type));
    }

    /**
     * 获取收藏列表（分页，按收藏时间倒序）
     */
    @GetMapping("/favorite/list")
    public Result<List<RandomGifVO>> getFavorites(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return Result.success(xhsWorkService.getFavorites(page, size));
    }

    /**
     * 随机获取同一作品的一整组套图GIF
     */
    @GetMapping("/randomGifGroup")
    public Result<List<RandomGifVO>> getRandomGifGroup() {
        log.info("获取随机套图GIF");
        List<RandomGifVO> list = xhsWorkService.getRandomGifGroup();
        if (list == null || list.isEmpty()) {
            return Result.error("暂无可用的套图GIF");
        }
        return Result.success(list);
    }

    /**
     * 获取所有GIF的ID列表
     */
    @GetMapping("/allGifIds")
    public Result<List<Long>> getAllGifIds() {
        log.info("获取所有GIF ID列表");
        List<Long> gifIds = xhsWorkService.getAllGifIds();
        return Result.success(gifIds);
    }

    /**
     * 根据ID获取GIF详情
     */
    @GetMapping("/gifById/{id}")
    public Result<RandomGifVO> getGifById(@PathVariable Long id) {
        log.info("根据ID获取GIF详情: {}", id);
        RandomGifVO gif = xhsWorkService.getGifById(id);
        if (gif == null) {
            throw new NotFoundException("未找到对应的GIF");
        }
        return Result.success(gif);
    }

    /**
     * 从 User-Agent 判断来源平台
     */
    private String getClientSource(HttpServletRequest request) {
        String ua = request.getHeader("User-Agent");
        if (ua != null) {
            if (ua.contains("Android")) return "android";
            if (ua.contains("iPhone") || ua.contains("iPad")) return "ios";
        }
        return "web";
    }

    /**
     * 获取客户端真实 IP（支持反向代理）
     */
    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 执行历史数据迁移
     */
    @PostMapping("/migrate")
    public Result<String> migrateData() {
        log.info("开始执行历史数据迁移");
        try {
            dataMigrationService.migrateData();
            String info = dataMigrationService.getMigrationInfo();
            return Result.success(info);
        } catch (Exception e) {
            log.error("数据迁移失败", e);
            return Result.error("迁移失败: " + e.getMessage());
        }
    }
}
