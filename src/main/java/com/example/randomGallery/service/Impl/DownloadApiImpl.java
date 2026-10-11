package com.example.randomGallery.service.Impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.example.randomGallery.common.Result;
import com.example.randomGallery.entity.QO.DownLoadQry;
import com.example.randomGallery.entity.VO.DownLoadInfo;
import com.example.randomGallery.service.DownloadApi;
import com.example.randomGallery.service.DownloadTaskService;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class DownloadApiImpl implements DownloadApi {

    /**
     * Cookie 校验探测请求的超时时间（毫秒），元数据调用应远快于真实下载
     */
    private static final int VALIDATE_TIMEOUT_MS = 15000;

    /**
     * 探测响应解析器：忽略未知字段，与 Spring 默认 ObjectMapper 行为一致
     */
    private static final ObjectMapper PROBE_OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private final DownloadTaskService downloadTaskService;
    private final DownloadTaskConsumer downloadTaskConsumer;

    @Value("${other.downloader.url}")
    private String xhsDetailUrl;

    @Value("${other.downloader.probe-url:}")
    private String probeUrl;

    @Override
    public void addDownloadTask(DownLoadQry qry) {
        // 1. 落库历史记录（状态=等待中）
        Long taskId = downloadTaskService.addTask(qry);
        // 2. 投递到单线程消费队列（消费时串行执行并加入 3~5s 随机延迟）
        downloadTaskConsumer.submit(taskId);
        log.info("下载任务已提交消费队列, taskId: {}, url: {}", taskId, qry.getUrl());
    }

    @Override
    public Result<Boolean> validateCookie(String cookie) {
        if (StrUtil.isBlank(cookie)) {
            return Result.error("Cookie 为空");
        }
        String value = cookie.trim();
        if (value.length() < 16 || value.length() > 8192
                || value.chars().anyMatch(c -> c < 32 || c >= 127)
                || java.util.Arrays.stream(value.split(";", -1)).anyMatch(part -> {
                    String[] pair = part.trim().split("=", 2);
                    return pair.length != 2 || StrUtil.isBlank(pair[0])
                            || pair[0].chars().anyMatch(Character::isWhitespace);
                })) {
            return Result.error("Cookie 格式不正确");
        }
        if (StrUtil.isBlank(probeUrl)) {
            return Result.error("未配置 other.downloader.probe-url");
        }
        try {
            // 仅元数据探测：download=false，不触发文件下载；全程不落库
            DownLoadQry qry = new DownLoadQry(probeUrl);
            qry.setDownload(false);
            qry.setCookie(value);
            try (var response = HttpRequest.post(xhsDetailUrl)
                    .body(JSONUtil.toJsonStr(qry))
                    .timeout(VALIDATE_TIMEOUT_MS)
                    .execute()) {
                if (!response.isOk()) {
                    log.warn("Cookie 校验下载器响应异常, status: {}", response.getStatus());
                    return Result.error("下载器暂不可用，请稍后重试");
                }
                boolean valid = isValidProbeResponse(response.body());
                log.info("Cookie 校验探测完成, valid: {}", valid);
                return Result.success(valid);
            }
        } catch (Exception e) {
            log.warn("Cookie 校验请求失败, type: {}", e.getClass().getSimpleName());
            return Result.error("校验服务请求失败，请检查下载器及探测作品配置");
        }
    }

    /**
     * 判定探测响应：能解析出 data 且作品ID 非空视为 Cookie 有效；解析失败一律无效
     */
    static boolean isValidProbeResponse(String body) {
        try {
            DownLoadInfo info = PROBE_OBJECT_MAPPER.readValue(body, DownLoadInfo.class);
            return info != null
                    && info.getData() != null
                    && StrUtil.isNotBlank(info.getData().getWorkId());
        } catch (Exception e) {
            return false;
        }
    }
}
