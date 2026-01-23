package com.example.randomGallery.service.Impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
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
    public XhsWorkPageVO pageXhsWorksWithFilter(int page, int pageSize, String authorId, Long tagId, String str) {
        // 分页查询基础表
        Page<XhsWorkBaseDO> pageParam = new Page<>(page, pageSize); // MyBatis-Plus 页码从1开始
        LambdaQueryWrapper<XhsWorkBaseDO> wrapper = Wrappers.lambdaQuery();
        wrapper.orderByDesc(XhsWorkBaseDO::getId);
        wrapper.eq(XhsWorkBaseDO::getIsDelete, false);
        // 如果指定了作者ID，添加筛选条件
        if (ObjectUtil.isNotEmpty(authorId)) {
            wrapper.eq(ObjectUtil.isNotNull(authorId), XhsWorkBaseDO::getAuthorId, authorId);
        }
        // 如果指定了标签ID，需要通过tag_work关联表查询work_id列表
        if (ObjectUtil.isNotEmpty(tagId)) {
            // 查询tag_work表获取该标签关联的所有work_id
            LambdaQueryWrapper<TagWorkDO> tagWorkWrapper = Wrappers.lambdaQuery();
            tagWorkWrapper.eq(TagWorkDO::getTagId, tagId);
            List<TagWorkDO> tagWorkList = tagWorkMapper.selectList(tagWorkWrapper);
            if (CollUtil.isEmpty(tagWorkList)) {
                XhsWorkPageVO emptyResult = new XhsWorkPageVO();
                emptyResult.setWorks(new ArrayList<>());
                emptyResult.setHasMore(false);
                return emptyResult;
            }
            // 提取work_id列表
            List<String> workIds = tagWorkList.stream().map(TagWorkDO::getWorkId).collect(Collectors.toList());
            // 添加work_id的in条件
            wrapper.in(XhsWorkBaseDO::getWorkId, workIds);
        }
        // 添加字符串查询条件
        if (StrUtil.isNotEmpty(str)) {
            wrapper.nested(i -> i
                    .like(XhsWorkBaseDO::getWorkTags, str)
                    .or()
                    .like(XhsWorkBaseDO::getWorkTitle, str)
                    .or()
                    .like(XhsWorkBaseDO::getWorkDescription, str)
                    .or()
                    .like(XhsWorkBaseDO::getAuthorNickname, str));
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
            mediaWrapper.eq(XhsWorkMediaDO::getIsDelete, false);
            List<XhsWorkMediaDO> allMedia = workMediaMapper.selectList(mediaWrapper);

            // 按 workId 分组
            Map<String, List<XhsWorkMediaDO>> mediaMap = allMedia.stream().collect(Collectors.groupingBy(XhsWorkMediaDO::getWorkId));

            // 组装 VO
            for (XhsWorkBaseDO baseDO : pageResult.getRecords()) {
                XhsWorkListVO vo = new XhsWorkListVO();
                vo.setId(baseDO.getId());
                vo.setWorkId(baseDO.getWorkId());
                vo.setWorkTitle(baseDO.getWorkTitle());
                vo.setAuthorNickname(baseDO.getAuthorNickname());
                vo.setPublishTime(baseDO.getPublishTime());

                // 获取该作品的媒体列表
                List<XhsWorkMediaDO> mediaList = mediaMap.getOrDefault(baseDO.getWorkId(), new ArrayList<>());

                // 先收集图片列表
                List<XhsWorkMediaDO> imageMediaList = mediaList.stream().filter(m -> MediaTypeEnum.IMAGE.equals(m.getMediaType())).toList();
                // 统计动图数量
                long gifCount = mediaList.stream().filter(m -> MediaTypeEnum.GIF.equals(m.getMediaType())).count();

                vo.setImageCount(imageMediaList.size()); // 直接用列表长度，无需count()
                vo.setGifCount((int) gifCount);

                // 随机获取封面（利用列表非空判断）
                if (!imageMediaList.isEmpty()) {
                    int randomIndex = ThreadLocalRandom.current().nextInt(imageMediaList.size());
                    vo.setCoverImageUrl(imageMediaList.get(randomIndex).getMediaUrl());
                }
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

    @Override
    public List<Long> getAllGifIds() {
        // 查询所有GIF类型且未删除的媒体ID
        LambdaQueryWrapper<XhsWorkMediaDO> wrapper = Wrappers.lambdaQuery();
        wrapper.select(XhsWorkMediaDO::getId)
                .eq(XhsWorkMediaDO::getMediaType, MediaTypeEnum.GIF)
                .and(w -> w.eq(XhsWorkMediaDO::getIsDelete, false).or()
                        .isNull(XhsWorkMediaDO::getIsDelete));

        List<XhsWorkMediaDO> mediaList = workMediaMapper.selectList(wrapper);

        if (CollUtil.isEmpty(mediaList)) {
            log.warn("数据库中没有可用的GIF");
            return Collections.emptyList();
        }

        return mediaList.stream()
                .map(XhsWorkMediaDO::getId)
                .collect(Collectors.toList());
    }

    @Override
    public RandomGifVO getGifById(Long id) {
        if (id == null) {
            log.warn("GIF ID为空");
            return null;
        }

        // 查询媒体信息
        XhsWorkMediaDO mediaDO = workMediaMapper.selectById(id);

        if (mediaDO == null) {
            log.warn("未找到ID为{}的GIF", id);
            return null;
        }

        // 转换为VO
        RandomGifVO vo = new RandomGifVO();
        vo.setId(mediaDO.getId());
        vo.setMediaUrl(mediaDO.getMediaUrl());
        vo.setWorkId(mediaDO.getWorkId());
        vo.setWorkBaseId(mediaDO.getWorkBaseId());

        // 查询作品基础信息
        if (mediaDO.getWorkBaseId() != null) {
            XhsWorkBaseDO baseDO = workBaseMapper.selectById(mediaDO.getWorkBaseId());
            if (baseDO != null) {
                vo.setWorkTitle(baseDO.getWorkTitle());
                vo.setAuthorNickname(baseDO.getAuthorNickname());
                vo.setAuthorId(baseDO.getAuthorId());
            }
        }

        return vo;
    }
}
