package com.example.randomGallery.server;


import com.example.randomGallery.server.mapper.PicServiceMapper;
import com.example.randomGallery.utils.ResettableTimer;
import com.example.randomGallery.utils.StrUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.Getter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Component
public class CacheService {

    private static final Logger log = LoggerFactory.getLogger(CacheService.class);

    @Getter
    private Integer maxId;

    @Getter
    private Integer minId;

    @Getter
    private Integer maxGroupId;

    @Getter
    private Integer minGroupId;

    private ResettableTimer resettableTimer;

    public static String sqlName = "cc_pic_all_dev";


    @Value("${config.env}")
    public static String defaultEnv;

    public String getDefaultEnv() {
        // 设置默认值
        if (StrUtils.isEmpty(defaultEnv)) {
            defaultEnv = "dev";
        }
        return defaultEnv;
    }

    public String getSqlName() {
        return sqlName;
    }

    public static final List<String> defaultList = Arrays.asList(
            "cc_pic_all_dev", "cc_pic_all_test", "cc_pic_all_prod"
    );

    public CacheService() {
        initTimer();
    }

    private void initTimer() {
        this.resettableTimer = new ResettableTimer(this, 5, "dev");
    }

    @Resource
    private PicServiceMapper picServiceMapper;

    @PostConstruct
    public void cachePicId() throws SQLException {
        // 缓存图片ID
        maxId = picServiceMapper.getMaxId(sqlName);
        minId = picServiceMapper.getMinId(sqlName);
        log.info("cache_minId:{} cache_maxId:{}", minId, maxId);

        // 缓存分组ID
        maxGroupId = picServiceMapper.getMaxGroupId(sqlName);
        minGroupId = picServiceMapper.getMinGroupId(sqlName);

        log.info("cache_minGroupId:{} cache_maxGroupId:{}", minGroupId, maxGroupId);
    }

    public Integer getRandomId() {
        return ThreadLocalRandom.current().nextInt(minId, maxId + 1);
    }

    public Integer getRandomGroupId() {
        return ThreadLocalRandom.current().nextInt(minGroupId, maxGroupId + 1);
    }

    public void switchSqlName(String env) throws SQLException {
        // 切换库
        String newSqlName = "cc_pic_all_" + env;

        // 验证环境名
        if (!defaultList.contains(newSqlName)) {
            log.error("无效的环境名:{}", env);
            return;
        }

        sqlName = newSqlName;

        defaultEnv = env;

        log.info("切换成功:{}", sqlName);

        // 刷新缓存
        this.cachePicId();

        // 刷新定时器
        this.resetTimer();
        log.warn("切换环境:{}完成", env);
    }

    public void resetTimer() {
        resettableTimer.reset();
    }
}