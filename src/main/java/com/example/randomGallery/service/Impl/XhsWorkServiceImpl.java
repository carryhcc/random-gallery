package com.example.randomGallery.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.randomGallery.entity.DO.TagWorkDO;
import com.example.randomGallery.entity.DO.XhsWorkBaseDO;
import com.example.randomGallery.entity.DO.XhsWorkMediaDO;
import com.example.randomGallery.entity.VO.RandomGifVO;
import com.example.randomGallery.entity.VO.XhsWorkDetailVO;
import com.example.randomGallery.entity.VO.XhsWorkListVO;
import com.example.randomGallery.entity.VO.XhsWorkPageVO;
import com.example.randomGallery.entity.common.MediaTypeEnum;
import com.example.randomGallery.service.XhsWorkService;
import com.example.randomGallery.service.mapper.TagWorkMapper;
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
        private final TagWorkMapper tagWorkMapper;

        @Override
        public XhsWorkPageVO pageXhsWorks(int page, int pageSize) {
                return pageXhsWorksWithFilter(page, pageSize, null, null);
        }

        @Override
        public XhsWorkPageVO pageXhsWorksWithFilter(int page, int pageSize, String authorId, Long tagId) {
                // 分页查询基础表
                Page<XhsWorkBaseDO> pageParam = new Page<>(page + 1, pageSize); // MyBatis-Plus 页码从1开始
                LambdaQueryWrapper<XhsWorkBaseDO> wrapper = Wrappers.lambdaQuery();
                wrapper.orderByDesc(XhsWorkBaseDO::getId); // 按 ID 倒序，后添加的排前面
                // 过滤已删除的数据 (兼容旧数据 null 情况)
                wrapper.and(w -> w.eq(XhsWorkBaseDO::getIsDelete, false).or().isNull(XhsWorkBaseDO::getIsDelete));

                // 如果指定了作者ID，添加筛选条件
                if (authorId != null && !authorId.trim().isEmpty()) {
                        wrapper.eq(XhsWorkBaseDO::getAuthorId, authorId);
                }

                // 如果指定了标签ID，需要通过tag_work关联表查询work_id列表
                if (tagId != null) {
                        // 查询tag_work表获取该标签关联的所有work_id
                        LambdaQueryWrapper<TagWorkDO> tagWorkWrapper = Wrappers.lambdaQuery();
                        tagWorkWrapper.eq(TagWorkDO::getTagId, tagId);
                        List<TagWorkDO> tagWorkList = tagWorkMapper.selectList(tagWorkWrapper);

                        if (CollUtil.isEmpty(tagWorkList)) {
                                // 如果该标签没有关联任何作品，返回空结果
                                XhsWorkPageVO emptyResult = new XhsWorkPageVO();
                                emptyResult.setWorks(new ArrayList<>());
                                emptyResult.setHasMore(false);
                                return emptyResult;
                        }

                        // 提取work_id列表
                        List<String> workIds = tagWorkList.stream()
                                        .map(TagWorkDO::getWorkId)
                                        .collect(Collectors.toList());

                        // 添加work_id的in条件
                        wrapper.in(XhsWorkBaseDO::getWorkId, workIds);
                }

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
                        // 过滤已删除的媒体
                        mediaWrapper.and(w -> w.eq(XhsWorkMediaDO::getIsDelete, false).or()
                                        .isNull(XhsWorkMediaDO::getIsDelete));
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

                if (ObjectUtil.isNull(baseDO)) {
                        log.warn("作品不存在：{}", workId);
                        return null;
                }

                // 查询所有媒体
                LambdaQueryWrapper<XhsWorkMediaDO> mediaWrapper = Wrappers.lambdaQuery();
                mediaWrapper.eq(XhsWorkMediaDO::getWorkId, workId)
                                .and(w -> w.eq(XhsWorkMediaDO::getIsDelete, false).or()
                                                .isNull(XhsWorkMediaDO::getIsDelete)) // 过滤已删除
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

        @Override
        public void deleteWork(String workId) {
                // 软删除作品
                XhsWorkBaseDO updateDO = new XhsWorkBaseDO();
                updateDO.setIsDelete(true);

                LambdaQueryWrapper<XhsWorkBaseDO> wrapper = Wrappers.lambdaQuery();
                wrapper.eq(XhsWorkBaseDO::getWorkId, workId);
                workBaseMapper.update(updateDO, wrapper);
        }

        @Override
        public void deleteMedia(Long id) {
                // 软删除媒体
                XhsWorkMediaDO updateDO = new XhsWorkMediaDO();
                updateDO.setId(id);
                updateDO.setIsDelete(true);
                workMediaMapper.updateById(updateDO);
        }

        @Override
        public RandomGifVO getRandomGif() {
                // 查询所有GIF类型且未删除的媒体
                LambdaQueryWrapper<XhsWorkMediaDO> wrapper = Wrappers.lambdaQuery();
                wrapper.eq(XhsWorkMediaDO::getMediaType, MediaTypeEnum.GIF)
                                .and(w -> w.eq(XhsWorkMediaDO::getIsDelete, false).or()
                                                .isNull(XhsWorkMediaDO::getIsDelete)); // 过滤已删除，兼容null

                List<XhsWorkMediaDO> gifList = workMediaMapper.selectList(wrapper);

                if (CollUtil.isEmpty(gifList)) {
                        log.warn("数据库中没有可用的GIF");
                        return null;
                }

                // 随机选择一个GIF（使用Java随机，避免数据库兼容性问题）
                int randomIndex = (int) (Math.random() * gifList.size());
                XhsWorkMediaDO randomGif = gifList.get(randomIndex);

                // 转换为VO
                RandomGifVO vo = new RandomGifVO();
                vo.setId(randomGif.getId());
                vo.setMediaUrl(randomGif.getMediaUrl());
                vo.setWorkId(randomGif.getWorkId());
                vo.setWorkBaseId(randomGif.getWorkBaseId());

                // 查询作品基础信息
                if (randomGif.getWorkBaseId() != null) {
                        XhsWorkBaseDO baseDO = workBaseMapper.selectById(randomGif.getWorkBaseId());
                        if (baseDO != null) {
                                vo.setWorkTitle(baseDO.getWorkTitle());
                                vo.setAuthorNickname(baseDO.getAuthorNickname());
                                vo.setAuthorId(baseDO.getAuthorId());
                        }
                }

                return vo;
        }
}
