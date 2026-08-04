package com.example.randomGallery.service.Impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONUtil;
import com.example.randomGallery.entity.DO.XhsDownloadTaskDO;
import com.example.randomGallery.entity.QO.DownLoadQry;
import com.example.randomGallery.entity.VO.DownLoadInfo;
import com.example.randomGallery.entity.common.DownloadTaskStatusEnum;
import com.example.randomGallery.service.DownloadTaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * 下载任务单线程消费器
 * <p>
 * 串行消费下载任务：连续添加解析任务时，每个任务执行前加入 3~5 秒随机延迟，
 * 避免瞬时打爆外部下载器。应用启动时会自动恢复数据库中遗留的「等待中」任务。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadTaskConsumer {

    /**
     * 单任务调用外部下载器前的最小/最大随机延迟（毫秒）
     */
    private static final int MIN_DELAY_MS = 3000;
    private static final int MAX_DELAY_MS = 5000;

    /**
     * 调用外部下载器的超时时间（毫秒），避免下载器异常时任务卡死在等待中
     */
    private static final int REQUEST_TIMEOUT_MS = 60000;

    private final DownloadTaskService downloadTaskService;
    private final XhsDataSaveService xhsDataSaveService;
    private final ObjectMapper objectMapper;

    @Value("${other.downloader.url}")
    private String xhsDetailUrl;

    private final LinkedBlockingQueue<Long> taskQueue = new LinkedBlockingQueue<>();

    private final ExecutorService consumerExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "download-task-consumer");
        t.setDaemon(true);
        return t;
    });

    private volatile boolean running = true;

    /**
     * 应用启动：恢复历史遗留的等待中任务
     */
    @PostConstruct
    public void start() {
        List<Long> pending = downloadTaskService.listPendingTaskIds();
        pending.forEach(taskQueue::offer);
        log.info("下载任务消费器启动, 恢复等待中任务 {} 个", pending.size());
        consumerExecutor.submit(this::consumeLoop);
    }

    @PreDestroy
    public void stop() {
        running = false;
        consumerExecutor.shutdownNow();
    }

    /**
     * 提交任务ID到消费队列
     */
    public void submit(Long taskId) {
        taskQueue.offer(taskId);
    }

    private void consumeLoop() {
        while (running) {
            try {
                Long taskId = taskQueue.poll(1, TimeUnit.SECONDS);
                if (taskId == null) {
                    continue;
                }
                XhsDownloadTaskDO task = downloadTaskService.getById(taskId);
                if (task == null || task.getStatus() != DownloadTaskStatusEnum.WAITING) {
                    continue;
                }
                process(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("消费下载任务异常", e);
            }
        }
        log.info("下载任务消费器已停止");
    }

    private void process(XhsDownloadTaskDO task) throws InterruptedException {
        // 连续解析时，每个任务执行前随机延迟 3~5 秒
        int delay = MIN_DELAY_MS + ThreadLocalRandom.current().nextInt(MAX_DELAY_MS - MIN_DELAY_MS + 1);
        log.info("开始处理下载任务 id: {}, url: {}, 延迟 {}ms", task.getId(), task.getUrl(), delay);
        Thread.sleep(delay);

        try {
            String result = HttpRequest.post(xhsDetailUrl)
                    .body(JSONUtil.toJsonStr(convertToQry(task)))
                    .timeout(REQUEST_TIMEOUT_MS)
                    .execute()
                    .body();
            xhsDataSaveService.saveXhsData(result);

            String workId = null;
            String workTitle = null;
            String workUrl = null;
            try {
                DownLoadInfo info = objectMapper.readValue(result, DownLoadInfo.class);
                if (info != null && info.getData() != null) {
                    workId = info.getData().getWorkId();
                    workTitle = info.getData().getWorkTitle();
                    workUrl = info.getData().getWorkUrl();
                }
            } catch (Exception e) {
                log.warn("解析下载结果作品信息失败, taskId: {}", task.getId(), e);
            }

            downloadTaskService.markCompleted(task.getId(), workId, workTitle, workUrl);
            log.info("下载任务处理完成, id: {}, workId: {}", task.getId(), workId);
        } catch (Exception e) {
            log.error("下载任务处理失败, id: {}, url: {}", task.getId(), task.getUrl(), e);
            downloadTaskService.markFailed(task.getId(), StrUtil.brief(e.getMessage(), 1000));
        }
    }

    private DownLoadQry convertToQry(XhsDownloadTaskDO task) {
        DownLoadQry qry = new DownLoadQry(task.getUrl());
        qry.setDownload(task.getParamsDownload());
        if (StrUtil.isNotBlank(task.getParamsIndex())) {
            qry.setIndex(JSONUtil.toList(task.getParamsIndex(), Integer.class));
        }
        qry.setCookie(task.getParamsCookie());
        qry.setProxy(task.getParamsProxy());
        qry.setSkip(task.getParamsSkip());
        return qry;
    }
}
