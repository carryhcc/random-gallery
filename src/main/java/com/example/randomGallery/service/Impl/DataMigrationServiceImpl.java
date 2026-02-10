package com.example.randomGallery.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.randomGallery.entity.DO.XhsWorkBaseDO;
import com.example.randomGallery.service.AuthorService;
import com.example.randomGallery.service.DataMigrationService;
import com.example.randomGallery.service.TagService;
import com.example.randomGallery.service.mapper.AuthorMapper;
import com.example.randomGallery.service.mapper.AuthorWorkMapper;
import com.example.randomGallery.service.mapper.TagMapper;
import com.example.randomGallery.service.mapper.TagWorkMapper;
import com.example.randomGallery.service.mapper.XhsWorkBaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 数据迁移服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataMigrationServiceImpl implements DataMigrationService {

    private static final int PAGE_SIZE = 1000;

    private final XhsWorkBaseMapper workBaseMapper;
    private final AuthorService authorService;
    private final TagService tagService;
    private final AuthorMapper authorMapper;
    private final AuthorWorkMapper authorWorkMapper;
    private final TagMapper tagMapper;
    private final TagWorkMapper tagWorkMapper;

    @Override
    public void migrateData() {
        log.info("开始执行数据迁移任务...");

        int totalCount = 0;
        int currentPage = 1;
        boolean hasMore = true;

        while (hasMore) {
            // 分页查询作品，避免一次性加载过多数据
            Page<XhsWorkBaseDO> page = workBaseMapper.selectPage(
                    new Page<>(currentPage, PAGE_SIZE),
                    Wrappers.<XhsWorkBaseDO>lambdaQuery().eq(XhsWorkBaseDO::getIsDelete, false)
            );

            List<XhsWorkBaseDO> workList = page.getRecords();
            if (workList.isEmpty()) {
                hasMore = false;
                break;
            }

            // 分批处理，每批使用独立事务
            processBatch(workList);

            totalCount += workList.size();
            log.info("已处理 {} 个作品 (第 {} 页)", totalCount, currentPage);

            currentPage++;
            hasMore = page.hasNext();
        }

        log.info("数据迁移任务完成！总共处理了 {} 个作品", totalCount);
        log.info(getMigrationInfo());
    }

    /**
     * 分批处理作品，每批使用独立事务
     */
    @Transactional(rollbackFor = Exception.class)
    public void processBatch(List<XhsWorkBaseDO> workList) {
        for (XhsWorkBaseDO work : workList) {
            String workId = work.getWorkId();
            String authorId = work.getAuthorId();
            String workTags = work.getWorkTags();

            // 处理作者信息
            if (authorId != null && !authorId.trim().isEmpty()) {
                authorService.saveOrUpdateAuthor(authorId, work.getAuthorNickname(), work.getAuthorUrl());
                authorService.createAuthorWorkRelation(authorId, workId);
            }

            // 处理标签
            if (workTags != null && !workTags.trim().isEmpty()) {
                tagService.processWorkTags(workTags, workId);
            }
        }
    }

    @Override
    public String getMigrationInfo() {
        long authorCount = authorMapper.selectCount(null);
        long authorWorkCount = authorWorkMapper.selectCount(null);
        long tagCount = tagMapper.selectCount(null);
        long tagWorkCount = tagWorkMapper.selectCount(null);

        return String.format(
                "迁移统计:\n" +
                        "- 作者总数: %d\n" +
                        "- 作者-作品关联数: %d\n" +
                        "- 标签总数: %d\n" +
                        "- 标签-作品关联数: %d",
                authorCount, authorWorkCount, tagCount, tagWorkCount);
    }
}
