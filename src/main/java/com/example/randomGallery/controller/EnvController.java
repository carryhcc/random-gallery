package com.example.randomGallery.controller;


import com.example.randomGallery.server.CacheService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.sql.SQLException;

@Controller
public class EnvController {

    @Resource
    private CacheService cacheService;

    @ResponseBody
    @GetMapping("/env")
    public void switchEnv(String env) throws SQLException {
        cacheService.switchSqlName(env);
    }

    @ResponseBody
    @GetMapping("/dev")
    public String switchEnvDev() throws SQLException {
        String env = "dev";
        cacheService.switchSqlName(env);
        System.out.println("切换环境:" + env);
        return "切换环境:" + env;
    }

    @ResponseBody
    @GetMapping("/test")
    public String switchEnvTest() throws SQLException {
        String env = "test";
        cacheService.switchSqlName(env);
        return "切换环境:" + env;
    }

    @ResponseBody
    @GetMapping("/prod")
    public String switchEnvProd() throws SQLException {
        String env = "prod";
        cacheService.switchSqlName(env);
        return "切换环境:" + env;
    }
}
