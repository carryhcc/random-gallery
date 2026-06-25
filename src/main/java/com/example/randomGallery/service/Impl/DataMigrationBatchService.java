package com.example.randomGallery.service.Impl;

import com.example.randomGallery.entity.DO.XhsWorkBaseDO;
import com.example.randomGallery.service.AuthorService;
import com.example.randomGallery.service.TagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 数据迁移批处理服务
 * 独立 Spring Bean，确保 @Transactional 通过 Spring 代理生效
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataMigrationBatchService {

    private final AuthorService authorService;
    private final TagService tagService;

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
}
