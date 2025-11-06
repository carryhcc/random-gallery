package com.example.randomGallery.controller;

import com.example.randomGallery.entity.VO.PicVO;
import com.example.randomGallery.service.CacheService;
import com.example.randomGallery.service.PicServiceApi;
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
public class PageController {

    private final CacheService cacheService;
    private final PicServiceApi picServiceApi;

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
     * 跳转到图片列表页面
     */
    @GetMapping("/showPicList")
    public ModelAndView showPicListPage(
            @RequestParam(value = "groupId", required = false) Integer groupId,
            @RequestParam(value = "groupName", required = false) String groupName) {
        log.debug("跳转到图片列表页面，groupId: {}, groupName: {}", groupId, groupName);
        ModelAndView modelAndView = new ModelAndView("picList");
        
        // 为模板传递初始数据
        if (groupId != null && groupName != null) {
            // 从分组列表跳转，有具体的套图信息
            modelAndView.addObject("groupId", groupId);
            modelAndView.addObject("groupName", groupName);
            modelAndView.addObject("isFromGroupList", true);
        } else if (groupId != null) {
            // 从主页跳转，只有groupId
            modelAndView.addObject("groupId", groupId);
            modelAndView.addObject("isFromGroupList", false);
        } else {
            // 随机套图，没有groupId
            modelAndView.addObject("isFromGroupList", false);
        }
        
        return modelAndView;
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
     * 跳转到随机画廊页面
     */
    @GetMapping("/randomGallery")
    public ModelAndView showRandomGalleryPage() {
        log.debug("跳转到随机画廊页面");
        return new ModelAndView("randomGallery");
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
