//package com.example.randomGallery.controller;
//
//import com.example.demo.server.CacheService;
//import org.noear.solon.annotation.*;
//
//import java.sql.SQLException;
//
//@Controller
//public class EnvController {
//
//    @Inject
//    CacheService cacheService;
//
//    @Get
//    @Socket
//    @Mapping("/switch")
//    public void switchEnv(String env) throws SQLException {
//        cacheService.switchSqlName(env);
//    }
//
//    @Mapping("/dev")
//    public String switchEnvDev() throws SQLException {
//        String env = "dev";
//        cacheService.switchSqlName(env);
//        return "切换环境:" + env;
//    }
//
//    @Mapping("/test")
//    public String switchEnvTest() throws SQLException {
//        String env = "test";
//        cacheService.switchSqlName(env);
//        return "切换环境:" + env;
//    }
//
//    @Mapping("/prod")
//    public String switchEnvProd() throws SQLException {
//        String env = "prod";
//        cacheService.switchSqlName(env);
//        return "切换环境:" + env;
//    }
//
//    /**
//     * 获取当前环境
//     */
//    @Mapping("/getEnv")
//    public String getEnv(){
//        String val = cacheService.getDefaultEnv();
//        return String.format(val);
//    }
//}
