package com.example.randomGallery.controller;

import com.example.randomGallery.annotation.PreventDuplicateSubmit;
import com.example.randomGallery.common.Result;
import com.example.randomGallery.entity.VO.PicCount;
import com.example.randomGallery.service.CacheService;
import com.example.randomGallery.service.GroupServiceApi;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;

/**
 * 系统管理控制器 - 处理系统配置、环境管理等功能
 */
@Slf4j
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemController {

    private final CacheService cacheService;
    private final GroupServiceApi groupServiceApi;
    private final com.example.randomGallery.config.PrivacyConfig privacyConfig;

    /**
     * 获取或者切换隐私模式状态
     * 如果传入 enabled 参数则设置状态，否则仅查询状态
     */
    @GetMapping("/privacy-mode")
    public Result<Boolean> handlePrivacyMode(@RequestParam(required = false) Boolean enabled) {
        if (enabled != null) {
            log.info("切换隐私模式: {}", enabled);
            privacyConfig.setEnabled(enabled);
            return Result.success("隐私模式已" + (enabled ? "开启" : "关闭"), enabled);
        }
        return Result.success(privacyConfig.getEnabled());
    }

    /**
     * 获取当前环境
     */
    @GetMapping("/env/current")
    public Result<String> getCurrentEnv() {
        log.debug("获取当前环境");
        String currentEnv = cacheService.getDefaultEnv();
        return Result.success("获取当前环境成功", currentEnv);
    }

    /**
     * 获取当前环境详情
     */
    @GetMapping("/env/currentInfo")
    public Result<PicCount> getCurrentEnvInfo() {
        log.debug("获取当前环境信息详情");
        PicCount picCount = cacheService.getDefaultEnvInfo();
        return Result.success("获取当前环境信息成功", picCount);
    }

    /**
     * 切换环境
     */
    @GetMapping("/env/switch")
    public Result<String> switchEnv(@RequestParam String env) throws SQLException {
        log.info("切换环境到: {}", env);
        cacheService.switchSqlName(env);
        return Result.success("环境切换成功", "当前环境: " + env);
    }

    /**
     * 切换到开发环境
     */
    @GetMapping("/env/dev")
    public Result<String> switchToDev() throws SQLException {
        log.info("切换到开发环境");
        return switchEnv("dev");
    }

    /**
     * 切换到测试环境
     */
    @GetMapping("/env/test")
    public Result<String> switchToTest() throws SQLException {
        log.info("切换到测试环境");
        return switchEnv("test");
    }

    /**
     * 切换到生产环境
     */
    @GetMapping("/env/prod")
    public Result<String> switchToProd() throws SQLException {
        log.info("切换到生产环境");
        return switchEnv("prod");
    }

    /**
     * 更新分组数据
     */
    @GetMapping("/up/group")
    public void upGroup() {
        groupServiceApi.updateGroupInfo();
    }

    /**
     * 测试防重复提交功能
     * 演示如何使用@PreventDuplicateSubmit注解
     */
    @PreventDuplicateSubmit
    @PostMapping("/test")
    public Result<String> testDuplicateSubmit(@RequestBody String testData) {
        log.info("测试防重复提交，收到数据: {}", testData);
        return Result.success("操作成功，数据已处理");
    }
}
