package com.example.randomGallery.service.Impl;
import com.example.randomGallery.entity.VO.PicVO;
import com.example.randomGallery.service.CacheService;
import com.example.randomGallery.service.PicServiceApi;
import com.example.randomGallery.service.mapper.PicServiceMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;



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
    public PicVO getInfoById(Integer id) {
        return picServiceMapper.getInfoById(cacheService.getPicSqlName(), id);
    }
}
