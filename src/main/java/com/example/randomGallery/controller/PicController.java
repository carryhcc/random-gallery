package com.example.randomGallery.controller;


import com.example.randomGallery.entity.VO.PicGroupVO;
import com.example.randomGallery.server.CacheService;
import com.example.randomGallery.server.PicServiceApi;
import jakarta.annotation.Resource;
import org.apache.ibatis.annotations.Param;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@Controller
public class PicController {


    @Resource
    CacheService cacheService;

    @Resource
    PicServiceApi picServiceApi;

//    @GetMapping("/")
//    public ModelAndView homePage() {
//        return new ModelAndView("1index.ftl1");
//    }

    /**
     * 获取当前环境
     */
    @ResponseBody
    @GetMapping("/getEnv")
    public String getEnv() {
        String val = cacheService.getDefaultEnv();
        return String.format(val);
    }

    /**
     * 跳转网页
     */
    @GetMapping("/showPic")
    public ModelAndView picTo() {
        Integer randomId = cacheService.getRandomId();
        String urlById = picServiceApi.getUrlById(randomId);
        return new ModelAndView("pic").addObject("url", urlById);
    }

    /**
     * 外部接口-返回随机单张图片地址
     */
    @GetMapping("/showPicOne")
    @ResponseBody
    public String picToOne() {
        return picServiceApi.getUrlById(cacheService.getRandomId());
    }


    /**
     * 获取随机套图
     */
    @GetMapping("/pic/list")
    public ResponseEntity<Map<String, String>> picList(@RequestParam(value = "groupId", required = false) Integer groupId) {
        cacheService.resetTimer();
        Integer finalGroupId = Optional.ofNullable(groupId).orElseGet(cacheService::getRandomGroupId);
        PicGroupVO randomGroupPicList = picServiceApi.getRandomGroupPicList(finalGroupId);
        if (randomGroupPicList == null) {
            return ResponseEntity.ok(Collections.emptyMap());
        }
        Map<String, String> map = Map.of(randomGroupPicList.getGroupName(), randomGroupPicList.getUrlList().toString());
        return ResponseEntity.ok(map);
    }

    @GetMapping("/showPicList")
    public ModelAndView showPicListPage() {
        return new ModelAndView("picList");
    }

    @GetMapping("/showQueryList")
    public ModelAndView showQueryList(@Param("groupId") Integer groupId) {
        return new ModelAndView("picList");
    }

    @GetMapping("/groupList")
    public ModelAndView showGroupList() {
        return new ModelAndView("group");
    }
}
