package com.example.randomGallery.controller;


import com.example.randomGallery.entity.VO.PicGroupVO;
import com.example.randomGallery.server.CacheService;
import com.example.randomGallery.server.PicServiceApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.ModelAndView;


import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

@Controller
public class PicController {


    @Resource
    CacheService cacheService;

    @Resource
    PicServiceApi picServiceApi;

    /**
     * 跳转网页
     *
     * @return
     * @throws SQLException
     */
    @GetMapping("/showPic")
    public ModelAndView picTo() throws SQLException {
        Integer randomId = cacheService.getRandomId();
        String urlById = picServiceApi.getUrlById(randomId);
        return new ModelAndView("pic").addObject("url", urlById);
    }


    /**
     * 获取随机套图
     *
     * @return
     * @throws SQLException
     */
    @GetMapping("/pic/list")
    @ResponseBody
    public Map<String, String> picList() {
        cacheService.resetTimer();
        Integer randomGroupId = cacheService.getRandomGroupId();
        PicGroupVO randomGroupPicList = picServiceApi.getRandomGroupPicList(randomGroupId);
        Map<String, String> map = new HashMap<>();
        map.put(randomGroupPicList.getGroupName(), randomGroupPicList.getUrlList().toString());
        return map;
    }

    @GetMapping("/showPicList")
    public ModelAndView showPicListPage() {
        return new ModelAndView("picList");
    }
}
