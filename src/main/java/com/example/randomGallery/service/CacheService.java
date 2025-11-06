package com.example.randomGallery.service;

import com.example.randomGallery.service.mapper.PicServiceMapper;
import com.example.randomGallery.utils.ResettableTimer;
import com.example.randomGallery.utils.StrUtils;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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
    private Long maxId;

    @Getter
    private Long minId;

    @Getter
    private Long maxGroupId;

    @Getter
    private Long minGroupId;

    private ResettableTimer resettableTimer;

    private String picSqlName = "cc_pic_all_dev";

    private String groupSqlName = "cc_pic_group_dev";

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

    public String getPicSqlName() {
        return picSqlName;
    }

    public String getGroupSqlName() {
        return groupSqlName;
    }

    @PostConstruct
    public void init() {
        try {
            initTimer();
            cachePicId();
            buildGroupIDList();
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
        maxId = picServiceMapper.getMaxId(picSqlName);
        minId = picServiceMapper.getMinId(picSqlName);
        log.info("图片ID缓存完成 - 最小值: {}, 最大值: {}", minId, maxId);

        // 缓存分组ID
        maxGroupId = picServiceMapper.getMaxGroupId(groupSqlName);
        minGroupId = picServiceMapper.getMinGroupId(groupSqlName);
        log.info("分组ID缓存完成 - 最小值: {}, 最大值: {}", minGroupId, maxGroupId);
    }

    /**
     * 获取随机图片ID
     */
    public Long getRandomId() {
        if (minId == null || maxId == null) {
            log.warn("图片ID缓存未初始化，返回null");
            return null;
        }
        return ThreadLocalRandom.current().nextLong(minId, maxId + 1);
    }

    /**
     * 获取随机分组ID
     */
    public Long getRandomGroupId() {
        if (minGroupId == null || maxGroupId == null) {
            log.warn("分组ID缓存未初始化，返回null");
            return null;
        }
        return ThreadLocalRandom.current().nextLong(minGroupId, maxGroupId + 1);
    }

    /**
     * 切换数据库环境
     */
    public void switchSqlName(String env) throws SQLException {
        String newPicSqlName = "cc_pic_all_" + env;
        String newGroupSqlName = "cc_pic_group_" + env;

        // 验证环境名
        if (!SUPPORTED_ENVS.contains(newPicSqlName)) {
            log.error("无效的环境名: {}, 支持的环境: {}", env, SUPPORTED_ENVS);
            throw new IllegalArgumentException("无效的环境名: " + env);
        }

        log.info("开始切换环境从 {} 到 {}", picSqlName, newPicSqlName);

        picSqlName = newPicSqlName;
        groupSqlName = newGroupSqlName;
        defaultEnv = env;

        // 刷新缓存
        cachePicId();
        // 刷新随机序列
        buildGroupIDList();
        // 刷新定时器
        resetTimer();

        log.info("环境切换完成: {}", newPicSqlName);
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

    // 内存存储随机序列
    @Getter
    private List<Long> shuffledSeq;
    // 总图片数
    @Getter
    public Long totalImageCount;
    // 总分组数
    @Getter
    public Long totalGroupCount;

    /**
     * 初始化随机序列和总图片数（懒加载：首次调用时初始化）
     */
    public void buildGroupIDList() {
        totalGroupCount = this.getMaxGroupId();
        List<Long> seq = new ArrayList<>();
        for (long i = 1; i <= totalGroupCount; i++) {
            seq.add(i);
        }
        log.warn("初始化随机数列:{}", totalGroupCount);
        Collections.shuffle(seq);
        shuffledSeq = seq;
    }
}