package com.example.randomGallery.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.example.randomGallery.entity.DO.XhsWorkMediaDO;
import com.example.randomGallery.entity.common.MediaTypeEnum;
import com.example.randomGallery.service.mapper.XhsWorkMediaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * GIF 资源有效性定时校验服务
 * 每日凌晨抽样 HEAD 校验一批 GIF URL，主动标记死链（is_dead=1）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GifValidationService {

    private final RestTemplate restTemplate;
    private final XhsWorkMediaMapper workMediaMapper;

    /** 每次最多校验的条数 */
    private static final int BATCH_SIZE = 100;

    /**
     * 每日凌晨 2:00 执行（cron: 秒 分 时 日 月 周）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @CacheEvict(value = "gifIds", key = "'all'")
    public void validateDeadLinks() {
        log.info("[GifValidation] 开始定时校验 GIF 资源有效性...");
        int marked = 0;
        int checked = 0;
        int deadCount = 0;

        try {
            // 查询所有未删除、未标记失效的 GIF
            LambdaQueryWrapper<XhsWorkMediaDO> wrapper = Wrappers.lambdaQuery();
            wrapper.select(XhsWorkMediaDO::getId, XhsWorkMediaDO::getMediaUrl)
                    .eq(XhsWorkMediaDO::getMediaType, MediaTypeEnum.GIF)
                    .eq(XhsWorkMediaDO::getIsDelete, false)
                    .eq(XhsWorkMediaDO::getIsDead, false);

            List<XhsWorkMediaDO> candidates = workMediaMapper.selectList(wrapper);
            if (candidates.isEmpty()) {
                log.info("[GifValidation] 没有待校验的 GIF 资源");
                return;
            }

            // 随机抽样，避免每次校验全量
            List<XhsWorkMediaDO> sampled = randomSample(candidates, BATCH_SIZE);
            checked = sampled.size();

            for (XhsWorkMediaDO media : sampled) {
                if (!isUrlAlive(media.getMediaUrl())) {
                    // 标记为失效
                    XhsWorkMediaDO update = new XhsWorkMediaDO();
                    update.setId(media.getId());
                    update.setIsDead(true);
                    workMediaMapper.updateById(update);
                    marked++;
                    log.debug("[GifValidation] 标记失效: id={}, url={}", media.getId(), media.getMediaUrl());
                } else {
                    deadCount++;
                }
            }

            log.info("[GifValidation] 校验完成：共校验 {} 条，新增标记失效 {} 条，存活 {} 条",
                    checked, marked, deadCount);
        } catch (Exception e) {
            log.error("[GifValidation] 定时校验异常", e);
        }
    }

    /**
     * HEAD 校验 URL 是否存活
     * 策略：HEAD 请求 + GET Range bytes=0-0 双重校验
     */
    private boolean isUrlAlive(String url) {
        if (url == null || url.isBlank()) {
            return false;
        }
        try {
            // 优先 HEAD（轻量）
            ResponseEntity<Void> headResp = restTemplate.exchange(url, HttpMethod.HEAD, null, Void.class);
            if (headResp.getStatusCode().is2xxSuccessful()) {
                return true;
            }
            if (headResp.getStatusCode().value() == 404 || headResp.getStatusCode().value() == 410) {
                return false;
            }
            // HEAD 返回 403/405 等（CDN 拒绝 HEAD），降级为 GET Range 检测
            return isUrlAliveByGet(url);
        } catch (Exception e) {
            // HEAD 请求异常，降级为 GET Range
            try {
                return isUrlAliveByGet(url);
            } catch (Exception ex) {
                log.debug("[GifValidation] 双重检测均失败: {}, error: {}", url, ex.getMessage());
                return false;
            }
        }
    }

    /**
     * GET Range bytes=0-0 检测 URL 是否可访问
     */
    private boolean isUrlAliveByGet(String url) {
        try {
            org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
            headers.add("Range", "bytes=0-0");
            org.springframework.http.HttpEntity<Void> entity = new org.springframework.http.HttpEntity<>(headers);
            ResponseEntity<Void> resp = restTemplate.exchange(url, HttpMethod.GET, entity, Void.class);
            int code = resp.getStatusCode().value();
            // 206 Partial Content = 正常; 200 OK 也算; 4xx/5xx = 失效
            return code == 200 || code == 206;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 从列表中随机抽取 max 个元素
     */
    private <T> List<T> randomSample(List<T> list, int max) {
        if (list.size() <= max) {
            return new ArrayList<>(list);
        }
        List<T> copy = new ArrayList<>(list);
        Collections.shuffle(copy, ThreadLocalRandom.current());
        return copy.subList(0, max);
    }
}
