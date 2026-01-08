package com.example.randomGallery.service.Impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.randomGallery.entity.DO.XhsWorkBaseDO;
import com.example.randomGallery.entity.DO.XhsWorkMediaDO;
import com.example.randomGallery.entity.QO.DownLoadQry;
import com.example.randomGallery.entity.VO.DownLoadInfo;
import com.example.randomGallery.entity.common.MediaTypeEnum;
import com.example.randomGallery.service.AuthorService;
import com.example.randomGallery.service.DownloadApi;
import com.example.randomGallery.service.TagService;
import com.example.randomGallery.service.mapper.XhsWorkBaseMapper;
import com.example.randomGallery.service.mapper.XhsWorkMediaMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadApiImpl implements DownloadApi {

    private final XhsWorkBaseMapper workBaseMapper;
    private final XhsWorkMediaMapper workMediaMapper;
    private final AuthorService authorService;
    private final TagService tagService;
    private final ObjectMapper objectMapper;

    // 注入自身以解决内部调用事务失效问题（或者将 saveXhsData 移至另一个 Service）
    private final ObjectProvider<DownloadApiImpl> selfProvider;

    @Value("${downloader.xhs.detail-url:http://${db.host}:5556/xhs/detail}")
    private String xhsDetailUrl;

    @Override
    public void addDownloadTask(DownLoadQry qry) {
        // 建议使用自定义线程池，避免使用默认的 ForkJoinPool.commonPool()
        CompletableFuture.runAsync(() -> {
            try {
                log.info("开始下载任务，参数: {}", qry);
                String result = HttpUtil.post(xhsDetailUrl, JSONUtil.toJsonStr(qry));

                // 通过代理对象调用，确保 @Transactional 生效
                Objects.requireNonNull(selfProvider.getIfAvailable()).saveXhsData(result);

            } catch (Exception e) {
                log.error("下载任务异步处理异常: {}", qry.getUrl(), e);
            }
        });
    }

    /**
     * 核心入库逻辑：支持事务回滚
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveXhsData(String jsonStr) {
        try {
            DownLoadInfo downLoadInfo = objectMapper.readValue(jsonStr, DownLoadInfo.class);
            if (downLoadInfo == null || downLoadInfo.getData() == null) {
                throw new IllegalArgumentException("解析数据为空");
            }

            DownLoadInfo.Data data = downLoadInfo.getData();
            String workId = data.getWorkId();

            // 1. 处理基础信息（利用 MyBatis Plus 的唯一索引冲突处理或先查后改）
            Long workBaseId = saveOrUpdateWorkBase(downLoadInfo);

            // 1.5 保存作者信息及关联
            if (StrUtil.isNotEmpty(data.getAuthorId())) {
                try {
                    authorService.saveOrUpdateAuthor(data.getAuthorId(), data.getAuthorNickname(), data.getAuthorUrl());
                    authorService.createAuthorWorkRelation(data.getAuthorId(), workId);
                } catch (Exception e) {
                    log.error("保存作者信息失败: {}", e.getMessage());
                    // 不中断主流程
                }
            }

            // 1.6 处理标签及关联
            if (StrUtil.isNotEmpty(data.getWorkTags())) {
                try {
                    tagService.processWorkTags(data.getWorkTags(), workId);
                } catch (Exception e) {
                    log.error("保存标签信息失败: {}", e.getMessage());
                    // 不中断主流程
                }
            }

            // 2. 收集并批量保存媒体数据
            List<XhsWorkMediaDO> mediaList = new ArrayList<>();
            collectMediaList(mediaList, workBaseId, workId, data.getDownloadUrls(), MediaTypeEnum.IMAGE);
            collectMediaList(mediaList, workBaseId, workId, data.getGifUrls(), MediaTypeEnum.GIF);

            if (CollUtil.isNotEmpty(mediaList)) {
                // 批量入库前过滤已存在的 URL (进一步防止重复)
                filterAndBatchInsertMedia(workId, mediaList);
            }

        } catch (Exception e) {
            log.error("数据入库异常", e);
            throw new RuntimeException("入库失败: " + e.getMessage());
        }
    }

    private Long saveOrUpdateWorkBase(DownLoadInfo info) {
        DownLoadInfo.Data data = info.getData();
        XhsWorkBaseDO existDO = workBaseMapper.selectOne(Wrappers.<XhsWorkBaseDO>lambdaQuery()
                .eq(XhsWorkBaseDO::getWorkId, data.getWorkId()));

        XhsWorkBaseDO workBaseDO = convertToWorkBaseDO(info);
        if (existDO != null) {
            workBaseDO.setId(existDO.getId());
            workBaseMapper.updateById(workBaseDO);
            return existDO.getId();
        } else {
            workBaseMapper.insert(workBaseDO);
            return workBaseDO.getId();
        }
    }

    private void collectMediaList(List<XhsWorkMediaDO> list, Long workBaseId, String workId,
            List<String> urls, MediaTypeEnum type) {
        if (CollUtil.isEmpty(urls))
            return;

        for (int i = 0; i < urls.size(); i++) {
            String url = urls.get(i);
            if (StrUtil.isBlank(url))
                continue;

            XhsWorkMediaDO mediaDO = new XhsWorkMediaDO();
            mediaDO.setWorkBaseId(workBaseId);
            mediaDO.setWorkId(workId);
            mediaDO.setMediaType(type);
            mediaDO.setMediaUrl(url);
            mediaDO.setSortIndex(i);
            mediaDO.setCreateTime(LocalDateTime.now());
            list.add(mediaDO);
        }
    }

    private void filterAndBatchInsertMedia(String workId, List<XhsWorkMediaDO> newList) {
        // 1. 一次性查出该作品已有的所有媒体URL
        List<XhsWorkMediaDO> existingMedia = workMediaMapper.selectList(Wrappers.<XhsWorkMediaDO>lambdaQuery()
                .eq(XhsWorkMediaDO::getWorkId, workId));

        Set<String> existUrls = existingMedia.stream()
                .map(XhsWorkMediaDO::getMediaUrl)
                .collect(Collectors.toSet());

        // 2. 过滤掉已存在的
        List<XhsWorkMediaDO> waitToInsert = newList.stream()
                .filter(item -> !existUrls.contains(item.getMediaUrl()))
                .collect(Collectors.toList());

        // 3. 批量插入
        if (CollUtil.isNotEmpty(waitToInsert)) {
            waitToInsert.forEach(workMediaMapper::insert);
            log.info("作品 {} 批量插入媒体数据 {} 条", workId, waitToInsert.size());
        }
    }

    private XhsWorkBaseDO convertToWorkBaseDO(DownLoadInfo info) {
        XhsWorkBaseDO res = new XhsWorkBaseDO();
        BeanUtil.copyProperties(info.getData(), res);
        BeanUtil.copyProperties(info.getParams(), res, "url"); // 排除重名但逻辑不同的字段
        res.setMessage(info.getMessage());
        res.setParamsUrl(info.getParams().getUrl());
        res.setTimestamp(BigDecimal.valueOf(info.getData().getTimestamp()));
        res.setCreateTime(LocalDateTime.now());
        return res;
    }
}