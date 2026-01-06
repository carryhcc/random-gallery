package com.example.randomGallery.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.json.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.randomGallery.entity.DO.XhsWorkBaseDO;
import com.example.randomGallery.entity.DO.XhsWorkMediaDO;
import com.example.randomGallery.entity.QO.DownLoadQry;
import com.example.randomGallery.entity.VO.DownLoadInfo;
import com.example.randomGallery.entity.common.MediaTypeEnum;
import com.example.randomGallery.service.DownloadApi;
import com.example.randomGallery.service.mapper.XhsWorkBaseMapper;
import com.example.randomGallery.service.mapper.XhsWorkMediaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadApiImpl implements DownloadApi {

    @Value("${db.host}")
    private String dbHost;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addDownloadTask(DownLoadQry qry) {
        CompletableFuture.runAsync(() -> {
            String paramMap = buildParamMap(qry);
            // 下载地址拼接
            String downUrl = "http://" + dbHost + ":5556" + "/xhs/detail";
            log.warn("下载地址：{}", downUrl);
            log.warn("请求参数：{}", paramMap);
            String result = HttpUtil.post(downUrl, paramMap);

            log.info("添加下载任务结果：{}", result);
            saveXhsData(result);
        });
    }


    private String buildParamMap(DownLoadQry qry) {
        // 构建请求参数
        HashMap<String, Object> paramMap = new HashMap<>();
        paramMap.put("url", qry.getUrl());
        if (qry.getDownload() == null) {
            paramMap.put("download", false);
        } else {
            paramMap.put("download", qry.getDownload());
        }
        if (qry.getIndex() == null) {
            paramMap.put("index", null);
        } else {
            paramMap.put("index", qry.getIndex());
        }
        if (qry.getCookie() == null) {
            paramMap.put("cookie", null);
        } else {
            paramMap.put("cookie", qry.getCookie());
        }
        if (qry.getProxy() == null) {
            paramMap.put("proxy", null);
        } else {
            paramMap.put("proxy", qry.getProxy());
        }
        if (qry.getSkip() == null) {
            paramMap.put("skip", false);
        } else {
            paramMap.put("skip", qry.getSkip());
        }
        return JSONUtil.toJsonStr(paramMap);
    }

    private final XhsWorkBaseMapper workBaseMapper;
    private final XhsWorkMediaMapper workMediaMapper;

    /**
     * 核心逻辑：JSON解析 → VO转DO → 入库（事务保证一致性）
     */
    @Transactional(rollbackFor = Exception.class) // 事务注解，异常时回滚
    public Long saveXhsData(String jsonStr) {
        try {
            // 步骤1：解析JSON字符串为VO对象
            DownLoadInfo downLoadInfo = JSONUtil.toBean(jsonStr, DownLoadInfo.class);
            if (downLoadInfo == null || downLoadInfo.getData() == null) {
                log.error("JSON解析结果为空");
                throw new IllegalArgumentException("JSON解析结果为空");
            }

            // 步骤2：VO转换为基础信息DO
            XhsWorkBaseDO workBaseDO = convertToWorkBaseDO(downLoadInfo);

            // 步骤3：先查询是否已存在该作品（根据work_id唯一索引），避免重复入库
            LambdaQueryWrapper<XhsWorkBaseDO> queryWrapper = Wrappers.lambdaQuery();
            queryWrapper.eq(XhsWorkBaseDO::getWorkId, workBaseDO.getWorkId());
            XhsWorkBaseDO existDO = workBaseMapper.selectOne(queryWrapper);
            Long workBaseId;
            if (existDO != null) {
                // 已存在，直接使用已有ID
                workBaseId = existDO.getId();
                log.info("作品{}已存在，无需重复入库，基础表ID：{}", workBaseDO.getWorkId(), workBaseId);
            } else {
                // 不存在，插入基础表
                workBaseMapper.insert(workBaseDO);
                workBaseId = workBaseDO.getId(); // 插入后自增ID会回写
                log.info("基础表入库成功，作品ID：{}，基础表ID：{}", workBaseDO.getWorkId(), workBaseId);
            }

            // 步骤4：处理图片地址（下载地址），转换为媒体DO并入库
            saveMediaData(workBaseId, downLoadInfo.getData().getDownloadUrls(), MediaTypeEnum.IMAGE);

            // 步骤5：处理动图地址，转换为媒体DO并入库
            saveMediaData(workBaseId, downLoadInfo.getData().getGifUrls(), MediaTypeEnum.GIF);

            return workBaseId;
        } catch (Exception e) {
            log.error("数据入库失败", e);
            throw new RuntimeException("数据入库失败", e);
        }
    }

    /**
     * VO转基础信息DO
     */
    private XhsWorkBaseDO convertToWorkBaseDO(DownLoadInfo downLoadInfo) {
        XhsWorkBaseDO workBaseDO = new XhsWorkBaseDO();
        // 1. 设置message
        workBaseDO.setMessage(downLoadInfo.getMessage());

        // 2. 设置params相关字段
        DownLoadInfo.Params params = downLoadInfo.getParams();
        if (params != null) {
            workBaseDO.setParamsUrl(params.getUrl());
            workBaseDO.setParamsDownload(params.isDownload());
            workBaseDO.setParamsIndex(params.getIndex());
            workBaseDO.setParamsCookie(params.getCookie());
            workBaseDO.setParamsProxy(params.getProxy());
            workBaseDO.setParamsSkip(params.isSkip());
        }

        // 3. 设置data相关字段
        DownLoadInfo.Data data = downLoadInfo.getData();
        workBaseDO.setCollectCount(data.getCollectCount());
        workBaseDO.setCommentCount(data.getCommentCount());
        workBaseDO.setShareCount(data.getShareCount());
        workBaseDO.setLikeCount(data.getLikeCount());
        workBaseDO.setWorkTags(data.getWorkTags());
        workBaseDO.setWorkId(data.getWorkId());
        workBaseDO.setWorkUrl(data.getWorkUrl());
        workBaseDO.setWorkTitle(data.getWorkTitle());
        workBaseDO.setWorkDescription(data.getWorkDescription());
        workBaseDO.setWorkType(data.getWorkType());
        workBaseDO.setPublishTime(data.getPublishTime());
        workBaseDO.setLastUpdateTime(data.getLastUpdateTime());
        // 时间戳转换为BigDecimal，避免精度丢失
        workBaseDO.setTimestamp(BigDecimal.valueOf(data.getTimestamp()));
        workBaseDO.setAuthorNickname(data.getAuthorNickname());
        workBaseDO.setAuthorId(data.getAuthorId());
        workBaseDO.setAuthorUrl(data.getAuthorUrl());

        return workBaseDO;
    }

    /**
     * 批量保存媒体地址数据（图片/动图）
     *
     * @param workBaseId 基础表ID
     * @param mediaUrls  媒体地址列表
     * @param mediaType  媒体类型
     */
    private void saveMediaData(Long workBaseId, List<String> mediaUrls, MediaTypeEnum mediaType) {
        if (CollUtil.isEmpty(mediaUrls)) {
            log.info("{}类型媒体地址为空，无需入库", mediaType.getValue());
            return;
        }

        // 遍历地址列表，按索引保存
        for (int i = 0; i < mediaUrls.size(); i++) {
            String mediaUrl = mediaUrls.get(i);
            // 跳过null值（动图地址中有大量null）
            if (mediaUrl == null) {
                continue;
            }

            // 构建媒体DO
            XhsWorkMediaDO mediaDO = new XhsWorkMediaDO();
            mediaDO.setWorkBaseId(workBaseId);
            mediaDO.setMediaType(mediaType);
            mediaDO.setMediaUrl(mediaUrl);
            mediaDO.setSortIndex(i); // 保留原始排序索引

            // 插入媒体表
            workMediaMapper.insert(mediaDO);
            log.debug("{}类型媒体入库成功，索引：{}，URL：{}", mediaType.getValue(), i, mediaUrl);
        }
        log.info("{}类型媒体入库完成，共{}条有效地址", mediaType.getValue(), mediaUrls.stream().filter(Objects::nonNull).count());
    }
}

