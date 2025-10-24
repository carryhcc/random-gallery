package com.example.randomGallery.controller;

import com.example.randomGallery.common.Result;
import com.example.randomGallery.server.CacheService;
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
}
