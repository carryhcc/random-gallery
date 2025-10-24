package com.example.randomGallery.server.Impl;

import com.example.randomGallery.entity.DO.PicInfoDO;
import com.example.randomGallery.entity.VO.PicGroupVO;
import com.example.randomGallery.server.CacheService;
import com.example.randomGallery.server.PicServiceApi;
import com.example.randomGallery.server.mapper.PicServiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

/**
 * 图片服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PicServiceApiImpl implements PicServiceApi {

    private final PicServiceMapper picServiceMapper;
    private final CacheService cacheService;

    @Override
    public String getUrlById(Integer id) {
        log.debug("根据ID获取图片URL，ID: {}", id);
        String url = picServiceMapper.getUrlById(cacheService.getSqlName(), id);
        log.debug("获取图片URL完成，URL: {}", url);
        return url;
    }

    @Override
    public PicGroupVO getRandomGroupPicList(Integer groupId) {
        log.debug("获取随机分组图片列表，分组ID: {}", groupId);
        String sqlName = cacheService.getSqlName();
        List<PicInfoDO> picList = picServiceMapper.getRandomGroupPicList(sqlName, groupId);
        
        if (picList == null || picList.isEmpty()) {
            log.warn("未找到分组ID为 {} 的图片", groupId);
            return new PicGroupVO();
        }
        
        // 提取分组名称（取第一个非空的图片名称）
        String groupName = picList.stream()
            .map(PicInfoDO::getPicName)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse("未知分组");
        
        // 提取图片URL列表
        List<String> urlList = picList.stream()
            .map(PicInfoDO::getPicUrl)
            .filter(Objects::nonNull)
            .toList();
        
        PicGroupVO result = new PicGroupVO(groupName, urlList);
        log.debug("获取随机分组图片列表完成，分组: {}，图片数量: {}", groupName, urlList.size());
        return result;
    }
}
