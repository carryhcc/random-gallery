package com.example.randomGallery.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.randomGallery.entity.DO.TagDO;
import com.example.randomGallery.entity.DO.TagWorkDO;
import com.example.randomGallery.entity.VO.TagVO;
import com.example.randomGallery.service.TagService;
import com.example.randomGallery.service.mapper.TagMapper;
import com.example.randomGallery.service.mapper.TagWorkMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 标签服务实现类
 */
@Service
public class TagServiceImpl implements TagService {

    @Autowired
    private TagMapper tagMapper;

    @Autowired
    private TagWorkMapper tagWorkMapper;

    @Override
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
            // 创建新标签
            TagDO tagDO = new TagDO();
            tagDO.setTagName(tagName);
            tagMapper.insert(tagDO);
            return tagDO.getId();
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
            tagWorkMapper.insert(tagWorkDO);
        }
    }

    @Override
    @Transactional
    public void processWorkTags(String workTags, String workId) {
        if (workTags == null || workTags.trim().isEmpty() || workId == null || workId.trim().isEmpty()) {
            return;
        }

        // 按空格拆分标签
        String[] tagArray = workTags.trim().split("\\s+");

        for (String tagName : tagArray) {
            tagName = tagName.trim();
            if (!tagName.isEmpty()) {
                // 获取或创建标签
                Long tagId = getOrCreateTag(tagName);
                if (tagId != null) {
                    // 创建标签作品关联
                    createTagWorkRelation(tagId, workId);
                }
            }
        }
    }
}
