package com.example.randomGallery.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

/**
 * 页面跳转控制器 - 处理页面跳转和视图渲染
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class ModelAndViewController {

    /**
     * 首页
     */
    @GetMapping("/")
    public String index() {
        return "forward:/index.html";
    }

    /**
     * 跳转到随机图片页面
     */
    @GetMapping("/showPic")
    public ModelAndView showPic() {
        log.debug("跳转到随机图片页面");
        return new ModelAndView("pic");
    }

    /**
     * 跳转到随机画廊页面
     */
    @GetMapping("/randomGallery")
    public ModelAndView showRandomGalleryPage() {
        log.debug("跳转到随机画廊页面");
        return new ModelAndView("randomGallery");
    }

    /**
     * 跳转到随机套图页面
     */
    @GetMapping("/showPicList")
    public ModelAndView showPicList() {
        log.debug("跳转到随机套图页面");
        return new ModelAndView("picList");
    }

    /**
     * 跳转到分组列表页面
     */
    @GetMapping("/groupList")
    public ModelAndView showGroupList() {
        log.debug("跳转到分组列表页面");
        return new ModelAndView("group");
    }

    /**
     * 跳转到查询列表页面
     */
    @GetMapping("/showQueryList")
    public ModelAndView showQueryList(@RequestParam("groupId") Integer groupId) {
        log.debug("跳转到查询列表页面，groupId: {}", groupId);
        return new ModelAndView("picList");
    }
}
