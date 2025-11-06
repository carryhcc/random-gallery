package com.example.randomGallery.utils;


import com.example.randomGallery.service.CacheService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class ResettableTimer implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(ResettableTimer.class);
    private static final AtomicInteger timerCount = new AtomicInteger(0); // 用于线程命名

    private final CacheService cacheService;
    private final long delayMillis;
    private final String targetEnv;
    private final ScheduledExecutorService scheduler;
    private ScheduledFuture<?> scheduledTaskFuture;

    public ResettableTimer(CacheService cacheService, long delayMinutes, String targetEnv) {
        this.cacheService = cacheService;
        // 将分钟转换为毫秒，修正了原始代码中的乘数
        this.delayMillis = delayMinutes * 60 * 1000;
        // 如果需要秒级切换，可以使用：
        // this.delayMillis = delayMinutes * 1000;
        this.targetEnv = targetEnv;
        // 创建单线程的 ScheduledExecutorService，并自定义线程工厂来命名线程
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ResettableTimer-" + targetEnv + "-" + timerCount.getAndIncrement());
            t.setDaemon(true); // 设置为守护线程，这样JVM退出时不会等待它
            return t;
        });

        // 初始调度任务
        scheduleNewTask();
    }

    private void scheduleNewTask() {
        // 先确保之前的任务（如果存在）被取消
        if (this.scheduledTaskFuture != null && !this.scheduledTaskFuture.isDone()) {
            this.scheduledTaskFuture.cancel(false); // false表示不中断正在执行的任务
        }

        // 如果是默认环境，则不调度实际的切换任务
        if (isDefaultEnv()) {
            log.info("当前为开发环境: {}，不调度环境切换任务。", targetEnv);
            return; // 不调度任务
        }

        Runnable task = () -> {
            try {
                // 在任务执行前再次检查是否为默认环境，以防在延迟期间配置发生变化
                // （虽然当前逻辑下targetEnv和defaultEnv是固定的，但作为一种防御性编程）
                if (isDefaultEnv()) {
                    log.info("任务执行时检测到当前为开发环境: {}，跳过切换。", targetEnv);
                    return;
                }
                cacheService.switchSqlName(targetEnv);
                log.info("定时器触发：已切换到 {} 环境", targetEnv);
            } catch (SQLException e) {
                log.error("定时器切换环境失败，目标环境: {}", targetEnv, e);
            } catch (Exception e) {
                log.error("定时器任务执行时发生未知错误，目标环境: {}", targetEnv, e);
            }
        };

        this.scheduledTaskFuture = scheduler.schedule(task, delayMillis, TimeUnit.MILLISECONDS);
        log.info("定时任务已设置，{} 后尝试切换到 {} 环境", formatDelayTime(delayMillis), targetEnv);
    }

    public void reset() {
        if (scheduler.isShutdown()) {
            log.warn("定时器已被关闭，无法重置。");
            return;
        }
        try {
            log.info("开始重置定时器，目标环境: {}", targetEnv);
            // 取消当前计划的任务（如果存在且未完成）
            if (this.scheduledTaskFuture != null && !this.scheduledTaskFuture.isDone()) {
                this.scheduledTaskFuture.cancel(false); // false表示不打断正在执行的任务
                log.debug("已取消之前的定时任务。");
            }

            // 重新调度任务
            scheduleNewTask();
            // scheduleNewTask内部已经有日志了，如果需要，这里可以再加一个总的重置成功日志
            // log.info("定时器已重置，{} 后切换到 {} 环境", formatDelayTime(delayMillis), targetEnv);
        } catch (Exception e) {
            log.error("重置定时器失败，目标环境: {}", targetEnv, e);
            // 发生异常时，可以考虑是否需要尝试恢复或关闭scheduler
        }
    }

    public Boolean isDefaultEnv() {
        return targetEnv.equalsIgnoreCase(cacheService.getDefaultEnv());
    }

    /**
     * 将毫秒级延迟时间格式化为人类可读的时间描述
     *
     * @param millis 延迟毫秒数
     * @return 格式化后的时间描述，如"3分钟20秒"、"1分钟"或"45秒"
     */
    private String formatDelayTime(long millis) {
        if (millis < 0) {
            return "无效的延迟";
        }
        long totalSeconds = millis / 1000;
        long minutes = totalSeconds / 60;
        long seconds = totalSeconds % 60;

        if (minutes > 0 && seconds > 0) {
            return String.format("%d分钟%d秒", minutes, seconds);
        } else if (minutes > 0) {
            return String.format("%d分钟", minutes);
        } else {
            return String.format("%d秒", seconds);
        }
    }

    /**
     * 关闭定时器并释放资源。
     * 实现 AutoCloseable 接口可以在 try-with-resources 语句中使用。
     */
    @Override
    public void close() {
        log.info("正在关闭 ResettableTimer (目标环境: {}) ...", targetEnv);
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown(); // 阻止新任务提交
            try {
                // 等待当前执行的任务结束，或者超时
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    log.warn("定时器任务在5秒内未完全终止，尝试强制关闭...");
                    scheduler.shutdownNow(); // 取消所有等待的任务并尝试中断当前执行的任务
                    if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                        log.error("定时器未能终止。");
                    }
                }
            } catch (InterruptedException ie) {
                log.warn("关闭定时器时被中断，强制关闭。");
                scheduler.shutdownNow();
                Thread.currentThread().interrupt(); // 保留中断状态
            }
            log.info("ResettableTimer (目标环境: {}) 已关闭。", targetEnv);
        }
    }
}