package com.example.randomGallery.service.Impl;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.randomGallery.entity.DO.XhsWorkBaseDO;
import com.example.randomGallery.entity.DO.XhsWorkMediaDO;
import com.example.randomGallery.entity.VO.XhsWorkDetailVO;
import com.example.randomGallery.entity.VO.XhsWorkListVO;
import com.example.randomGallery.entity.VO.XhsWorkPageVO;
import com.example.randomGallery.entity.common.MediaTypeEnum;
import com.example.randomGallery.service.XhsWorkService;
import com.example.randomGallery.service.mapper.XhsWorkBaseMapper;
import com.example.randomGallery.service.mapper.XhsWorkMediaMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 作品查询服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class XhsWorkServiceImpl implements XhsWorkService {

        private final XhsWorkBaseMapper workBaseMapper;
        private final XhsWorkMediaMapper workMediaMapper;

        @Override
        public XhsWorkPageVO pageXhsWorks(int page, int pageSize) {
                // 分页查询基础表
                Page<XhsWorkBaseDO> pageParam = new Page<>(page + 1, pageSize); // MyBatis-Plus 页码从1开始
                LambdaQueryWrapper<XhsWorkBaseDO> wrapper = Wrappers.lambdaQuery();
                wrapper.orderByDesc(XhsWorkBaseDO::getId); // 按 ID 倒序，后添加的排前面
                Page<XhsWorkBaseDO> pageResult = workBaseMapper.selectPage(pageParam, wrapper);

                // 转换为 VO
                List<XhsWorkListVO> voList = new ArrayList<>();
                if (CollUtil.isNotEmpty(pageResult.getRecords())) {
                        // 批量查询所有作品的媒体数据
                        List<String> workIds = pageResult.getRecords().stream()
                                        .map(XhsWorkBaseDO::getWorkId)
                                        .collect(Collectors.toList());

                        LambdaQueryWrapper<XhsWorkMediaDO> mediaWrapper = Wrappers.lambdaQuery();
                        mediaWrapper.in(XhsWorkMediaDO::getWorkId, workIds);
                        List<XhsWorkMediaDO> allMedia = workMediaMapper.selectList(mediaWrapper);

                        // 按 workId 分组
                        Map<String, List<XhsWorkMediaDO>> mediaMap = allMedia.stream()
                                        .collect(Collectors.groupingBy(XhsWorkMediaDO::getWorkId));

                        // 组装 VO
                        for (XhsWorkBaseDO baseDO : pageResult.getRecords()) {
                                XhsWorkListVO vo = new XhsWorkListVO();
                                vo.setId(baseDO.getId());
                                vo.setWorkId(baseDO.getWorkId());
                                vo.setWorkTitle(baseDO.getWorkTitle());
                                vo.setAuthorNickname(baseDO.getAuthorNickname());
                                vo.setPublishTime(baseDO.getPublishTime());

                                // 获取该作品的媒体列表
                                List<XhsWorkMediaDO> mediaList = mediaMap.getOrDefault(baseDO.getWorkId(),
                                                new ArrayList<>());

                                // 统计图片和动图数量
                                long imageCount = mediaList.stream()
                                                .filter(m -> MediaTypeEnum.IMAGE.equals(m.getMediaType())).count();
                                long gifCount = mediaList.stream()
                                                .filter(m -> MediaTypeEnum.GIF.equals(m.getMediaType())).count();

                                vo.setImageCount((int) imageCount);
                                vo.setGifCount((int) gifCount);

                                // 获取第一张图片作为封面
                                mediaList.stream()
                                                .filter(m -> MediaTypeEnum.IMAGE.equals(m.getMediaType()))
                                                .min(Comparator.comparingInt(XhsWorkMediaDO::getSortIndex))
                                                .ifPresent(m -> vo.setCoverImageUrl(m.getMediaUrl()));
                                voList.add(vo);
                        }
                }

                // 构建返回结果
                XhsWorkPageVO result = new XhsWorkPageVO();
                result.setWorks(voList);
                result.setHasMore(pageResult.hasNext());

                return result;
        }

        @Override
        public XhsWorkDetailVO getXhsWorkDetail(String workId) {
                // 查询基础信息
                LambdaQueryWrapper<XhsWorkBaseDO> baseWrapper = Wrappers.lambdaQuery();
                baseWrapper.eq(XhsWorkBaseDO::getWorkId, workId);
                XhsWorkBaseDO baseDO = workBaseMapper.selectOne(baseWrapper);

                if (baseDO == null) {
                        log.warn("作品不存在：{}", workId);
                        return null;
                }

                // 查询所有媒体
                LambdaQueryWrapper<XhsWorkMediaDO> mediaWrapper = Wrappers.lambdaQuery();
                mediaWrapper.eq(XhsWorkMediaDO::getWorkId, workId)
                                .orderByAsc(XhsWorkMediaDO::getSortIndex); // 按索引排序
                List<XhsWorkMediaDO> allMedia = workMediaMapper.selectList(mediaWrapper);

                // 分类：图片和动图
                List<XhsWorkMediaDO> images = allMedia.stream()
                                .filter(m -> MediaTypeEnum.IMAGE.equals(m.getMediaType()))
                                .collect(Collectors.toList());

                List<XhsWorkMediaDO> gifs = allMedia.stream()
                                .filter(m -> MediaTypeEnum.GIF.equals(m.getMediaType()))
                                .collect(Collectors.toList());

                // 组装 VO
                XhsWorkDetailVO vo = new XhsWorkDetailVO();
                vo.setBaseInfo(baseDO);
                vo.setImages(images);
                vo.setGifs(gifs);

                return vo;
        }
}
