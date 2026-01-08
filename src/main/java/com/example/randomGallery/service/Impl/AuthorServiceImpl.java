package com.example.randomGallery.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.randomGallery.entity.DO.AuthorDO;
import com.example.randomGallery.entity.DO.AuthorWorkDO;
import com.example.randomGallery.entity.VO.AuthorVO;
import com.example.randomGallery.service.AuthorService;
import com.example.randomGallery.service.mapper.AuthorMapper;
import com.example.randomGallery.service.mapper.AuthorWorkMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 作者服务实现类
 */
@Service
public class AuthorServiceImpl implements AuthorService {

    @Autowired
    private AuthorMapper authorMapper;

    @Autowired
    private AuthorWorkMapper authorWorkMapper;

    @Override
    public List<AuthorVO> getAllAuthors() {
        // 使用自定义SQL一次性查询作者及其作品数量，避免N+1问题
        return authorMapper.selectAuthorsWithWorkCount();
    }

    @Override
    @Transactional
    public void saveOrUpdateAuthor(String authorId, String authorNickname, String authorUrl) {
        if (authorId == null || authorId.trim().isEmpty()) {
            return;
        }

        // 查询是否已存在
        QueryWrapper<AuthorDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("author_id", authorId);
        AuthorDO existingAuthor = authorMapper.selectOne(queryWrapper);

        if (existingAuthor != null) {
            // 更新作者信息
            existingAuthor.setAuthorNickname(authorNickname);
            existingAuthor.setAuthorUrl(authorUrl);
            authorMapper.updateById(existingAuthor);
        } else {
            // 新增作者
            AuthorDO authorDO = new AuthorDO();
            authorDO.setAuthorId(authorId);
            authorDO.setAuthorNickname(authorNickname);
            authorDO.setAuthorUrl(authorUrl);
            authorMapper.insert(authorDO);
        }
    }

    @Override
    @Transactional
    public void createAuthorWorkRelation(String authorId, String workId) {
        if (authorId == null || authorId.trim().isEmpty() || workId == null || workId.trim().isEmpty()) {
            return;
        }

        // 检查关联关系是否已存在
        QueryWrapper<AuthorWorkDO> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("author_id", authorId)
                .eq("work_id", workId);
        Long count = authorWorkMapper.selectCount(queryWrapper);

        if (count == 0) {
            // 创建新的关联关系
            AuthorWorkDO authorWorkDO = new AuthorWorkDO();
            authorWorkDO.setAuthorId(authorId);
            authorWorkDO.setWorkId(workId);
            authorWorkMapper.insert(authorWorkDO);
        }
    }
}
