package com.example.randomGallery.server;

import com.example.randomGallery.server.mapper.PicServiceMapper;
import com.example.randomGallery.utils.ResettableTimer;
import com.example.randomGallery.utils.StrUtils;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 缓存服务类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheService {

    private final PicServiceMapper picServiceMapper;

    @Getter
    private Integer maxId;

    @Getter
    private Integer minId;

    @Getter
    private Integer maxGroupId;

    @Getter
    private Integer minGroupId;

    private ResettableTimer resettableTimer;

    private String sqlName = "cc_pic_all_dev";

    @Value("${config.env}")
    private String defaultEnv;

    /**
     * 支持的环境列表
     */
    private static final List<String> SUPPORTED_ENVS = Arrays.asList(
            "cc_pic_all_dev", "cc_pic_all_test", "cc_pic_all_prod"
    );

    public String getDefaultEnv() {
        return StrUtils.isEmpty(defaultEnv) ? "dev" : defaultEnv;
    }

    public String getSqlName() {
        return sqlName;
    }

    @PostConstruct
    public void init() {
        try {
            initTimer();
            cachePicId();
            log.info("缓存服务初始化完成，当前环境: {}", getDefaultEnv());
        } catch (SQLException e) {
            log.error("缓存服务初始化失败", e);
            throw new RuntimeException("缓存服务初始化失败", e);
        }
    }

    private void initTimer() {
        this.resettableTimer = new ResettableTimer(this, 5, "dev");
    }

    @PostConstruct
    public void cachePicId() throws SQLException {
        log.info("开始缓存图片ID和分组ID...");
        
        // 缓存图片ID
        maxId = picServiceMapper.getMaxId(sqlName);
        minId = picServiceMapper.getMinId(sqlName);
        log.info("图片ID缓存完成 - 最小值: {}, 最大值: {}", minId, maxId);

        // 缓存分组ID
        maxGroupId = picServiceMapper.getMaxGroupId(sqlName);
        minGroupId = picServiceMapper.getMinGroupId(sqlName);
        log.info("分组ID缓存完成 - 最小值: {}, 最大值: {}", minGroupId, maxGroupId);
    }

    /**
     * 获取随机图片ID
     */
    public Integer getRandomId() {
        if (minId == null || maxId == null) {
            log.warn("图片ID缓存未初始化，返回null");
            return null;
        }
        return ThreadLocalRandom.current().nextInt(minId, maxId + 1);
    }

    /**
     * 获取随机分组ID
     */
    public Integer getRandomGroupId() {
        if (minGroupId == null || maxGroupId == null) {
            log.warn("分组ID缓存未初始化，返回null");
            return null;
        }
        return ThreadLocalRandom.current().nextInt(minGroupId, maxGroupId + 1);
    }

    /**
     * 切换数据库环境
     */
    public void switchSqlName(String env) throws SQLException {
        String newSqlName = "cc_pic_all_" + env;

        // 验证环境名
        if (!SUPPORTED_ENVS.contains(newSqlName)) {
            log.error("无效的环境名: {}, 支持的环境: {}", env, SUPPORTED_ENVS);
            throw new IllegalArgumentException("无效的环境名: " + env);
        }

        log.info("开始切换环境从 {} 到 {}", sqlName, newSqlName);
        
        sqlName = newSqlName;
        defaultEnv = env;

        // 刷新缓存
        cachePicId();

        // 刷新定时器
        resetTimer();
        
        log.info("环境切换完成: {}", sqlName);
    }

    /**
     * 重置定时器
     */
    public void resetTimer() {
        if (resettableTimer != null) {
            resettableTimer.reset();
            log.debug("定时器已重置");
        }
    }
}