package com.example.randomGallery.service;

import cn.hutool.core.convert.Convert;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.randomGallery.config.EnvContext;
import com.example.randomGallery.entity.DO.GroupDO;
import com.example.randomGallery.entity.DO.PicDO;
import com.example.randomGallery.entity.VO.PicCount;
import com.example.randomGallery.service.mapper.GroupServiceMapper;
import com.example.randomGallery.service.mapper.PicServiceMapper;
import com.example.randomGallery.utils.ResettableTimer;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 缓存服务类
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CacheService {

    private final GroupServiceMapper groupServiceMapper;
    private final PicServiceMapper picServiceMapper;
    private final EnvContext envContext;

    @Getter
    private Long maxId;

    @Getter
    private Long minId;

    @Getter
    private Long maxGroupId;

    @Getter
    private Long minGroupId;

    private ResettableTimer resettableTimer;

    @Getter
    private String picSqlName = "cc_pic_all_dev";

    @Getter
    private String groupSqlName = "cc_pic_group_dev";

    /**
     * 支持的环境列表
     */
    private static final List<String> SUPPORTED_ENVS = Arrays.asList(
            "cc_pic_all_dev", "cc_pic_all_test", "cc_pic_all_prod");

    public String getDefaultEnv() {
        return envContext.getCurrentEnv();
    }

    /**
     * 获取当前环境信息
     */
    public PicCount getDefaultEnvInfo() {
        PicCount picCount = new PicCount();
        picCount.setEnv(getDefaultEnv());
        picCount.setGroupCount(maxGroupId);
        picCount.setPicCount(maxId);
        return picCount;
    }

    @PostConstruct
    public void init() {
        try {
            // 初始化环境名称
            String env = getDefaultEnv();
            this.picSqlName = "cc_pic_all_" + env;
            this.groupSqlName = "cc_pic_group_" + env;

            initTimer();
            cachePicId();
            buildGroupIDList();
            log.info("缓存服务初始化完成，当前环境: {}", env);
        } catch (SQLException e) {
            log.error("缓存服务初始化失败", e);
            throw new RuntimeException("缓存服务初始化失败", e);
        }
    }

    private void initTimer() {
        this.resettableTimer = new ResettableTimer(this, 10, "dev");
    }

    public void cachePicId() throws SQLException {
        log.info("开始缓存图片ID和分组ID...");

        // 缓存图片ID
        PicDO maxPic = picServiceMapper
                .selectOne(new QueryWrapper<PicDO>().select("id").orderByDesc("id").last("LIMIT 1"));
        maxId = maxPic != null ? Long.valueOf(maxPic.getId()) : 0L;

        PicDO minPic = picServiceMapper
                .selectOne(new QueryWrapper<PicDO>().select("id").orderByAsc("id").last("LIMIT 1"));
        minId = minPic != null ? Long.valueOf(minPic.getId()) : 0L;

        log.info("图片ID缓存完成 - 最小值: {}, 最大值: {}", minId, maxId);

        // 缓存分组ID (from pic_info)
        PicDO maxGroupPic = picServiceMapper
                .selectOne(new QueryWrapper<PicDO>().select("group_id").orderByDesc("group_id").last("LIMIT 1"));
        maxGroupId = maxGroupPic != null ? maxGroupPic.getGroupId() : 0L;

        PicDO minGroupPic = picServiceMapper
                .selectOne(new QueryWrapper<PicDO>().select("group_id").orderByAsc("group_id").last("LIMIT 1"));
        minGroupId = minGroupPic != null ? minGroupPic.getGroupId() : 0L;

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
        if (minId > maxId) {
            log.warn("图片ID范围无效 (min: {}, max: {})", minId, maxId);
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
        if (minGroupId > maxGroupId) {
            log.warn("分组ID范围无效 (min: {}, max: {})", minGroupId, maxGroupId);
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
        envContext.setCurrentEnv(env); // update context

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
    private List<Long> shuffledSeq;

    /**
     * 获取随机序列的不可变副本，防止外部修改
     */
    public List<Long> getShuffledSeq() {
        return shuffledSeq != null ? Collections.unmodifiableList(shuffledSeq) : Collections.emptyList();
    }
    // 总图片数
    @Getter
    public Integer totalImageCount;
    // 总分组数
    @Getter
    public Integer totalGroupCount;

    /**
     * 初始化随机序列和总图片数（懒加载：首次调用时初始化）
     */
    public void buildGroupIDList() {
        // 从数据库中查询所有实际存在的group_id
        List<Object> groupIds = groupServiceMapper.selectObjs(new QueryWrapper<GroupDO>().select("group_id"));

        List<Long> groupIdList = groupIds.stream().map(obj -> Convert.toLong(obj.toString())).collect(Collectors.toList());

        totalGroupCount = groupIdList.size();
        // 打乱顺序
        Collections.shuffle(groupIdList);
        shuffledSeq = groupIdList;
        log.info("初始化随机数列: {}", totalGroupCount);
    }
}
