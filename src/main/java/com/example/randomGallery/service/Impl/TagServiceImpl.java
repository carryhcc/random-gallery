package com.example.randomGallery.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.randomGallery.entity.DO.TagDO;
import com.example.randomGallery.entity.DO.TagWorkDO;
import com.example.randomGallery.entity.VO.TagVO;
import com.example.randomGallery.service.TagService;
import com.example.randomGallery.service.mapper.TagMapper;
import com.example.randomGallery.service.mapper.TagWorkMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 标签服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {

    private final TagMapper tagMapper;

    private final TagWorkMapper tagWorkMapper;

    @Override
    @Cacheable(value = "tags", unless = "#result == null || #result.isEmpty()")
    public List<TagVO> getAllTags() {
        // 使用自定义SQL一次性查询标签及其作品数量，避免N+1问题
        return tagMapper.selectTagsWithWorkCount();
    }

    @Override
    @Transactional
    public Long getOrCreateTag(String tagName) {
        if (tagName == null || tagName.trim().isEmpty()) {
            return null;
        }

        // 规范化标签名（去除前后空格）
        tagName = tagName.trim();

        // 查询标签是否已存在
        QueryWrapper<TagDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tag_name", tagName);
        TagDO existingTag = tagMapper.selectOne(queryWrapper);

        if (existingTag != null) {
            return existingTag.getId();
        } else {
            // 创建新标签（并发场景下可能重复插入，捕获DuplicateKeyException）
            try {
                TagDO tagDO = new TagDO();
                tagDO.setTagName(tagName);
                tagDO.setCreateTime(LocalDateTime.now());
                tagMapper.insert(tagDO);
                return tagDO.getId();
            } catch (DuplicateKeyException e) {
                log.debug("标签 {} 已存在（并发插入），重新查询", tagName);
                existingTag = tagMapper.selectOne(queryWrapper);
                return existingTag != null ? existingTag.getId() : null;
            }
        }
    }

    @Override
    @Transactional
    public void createTagWorkRelation(Long tagId, String workId) {
        if (tagId == null || workId == null || workId.trim().isEmpty()) {
            return;
        }

        // 检查关联关系是否已存在
        QueryWrapper<TagWorkDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("tag_id", tagId)
                .eq("work_id", workId);
        Long count = tagWorkMapper.selectCount(queryWrapper);

        if (count == 0) {
            // 创建新的关联关系
            TagWorkDO tagWorkDO = new TagWorkDO();
            tagWorkDO.setTagId(tagId);
            tagWorkDO.setWorkId(workId);
            tagWorkDO.setCreateTime(LocalDateTime.now());
            tagWorkMapper.insert(tagWorkDO);
        }
    }

    @Override
    public List<TagVO> searchTags(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return Collections.emptyList();
        }
        return tagMapper.selectByKeyword(keyword.trim());
    }

    @Override
    @Transactional
    public void processWorkTags(String workTags, String workId) {
        if (workTags == null || workTags.trim().isEmpty() || workId == null || workId.trim().isEmpty()) {
            return;
        }

        List<String> tagNames = Arrays.stream(workTags.trim().split("\\s+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (tagNames.isEmpty()) {
            return;
        }

        // Batch query existing tags (1 SELECT instead of N)
        List<TagDO> existingTags = tagMapper.selectList(
                new QueryWrapper<TagDO>().in("tag_name", tagNames));
        Map<String, Long> nameToId = existingTags.stream()
                .collect(Collectors.toMap(TagDO::getTagName, TagDO::getId));

        // Insert missing tags individually (safe under concurrent transactions)
        for (String tagName : tagNames) {
            if (!nameToId.containsKey(tagName)) {
                Long id = getOrCreateTag(tagName);
                if (id != null) {
                    nameToId.put(tagName, id);
                }
            }
        }

        List<Long> allTagIds = new ArrayList<>(nameToId.values());
        if (allTagIds.isEmpty()) {
            return;
        }

        // Batch query existing relations (1 SELECT instead of N)
        Set<Long> existingTagIds = tagWorkMapper.selectList(
                        new QueryWrapper<TagWorkDO>()
                                .eq("work_id", workId)
                                .in("tag_id", allTagIds))
                .stream()
                .map(TagWorkDO::getTagId)
                .collect(Collectors.toSet());

        // Insert only missing relations
        for (Long tagId : allTagIds) {
            if (!existingTagIds.contains(tagId)) {
                try {
                    TagWorkDO rel = new TagWorkDO();
                    rel.setTagId(tagId);
                    rel.setWorkId(workId);
                    rel.setCreateTime(LocalDateTime.now());
                    tagWorkMapper.insert(rel);
                } catch (DuplicateKeyException e) {
                    log.debug("标签作品关联 tagId={}, workId={} 已存在（并发插入），跳过", tagId, workId);
                }
            }
        }
    }
}
