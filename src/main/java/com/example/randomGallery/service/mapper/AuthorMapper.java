package com.example.randomGallery.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.randomGallery.entity.DO.AuthorDO;
import com.example.randomGallery.entity.VO.AuthorVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 作者Mapper接口
 */
@Mapper
public interface AuthorMapper extends BaseMapper<AuthorDO> {

    @Select("SELECT a.author_id as authorId, a.author_nickname as authorNickname, a.author_url as authorUrl, COUNT(aw.work_id) as workCount "
            +
            "FROM author a " +
            "LEFT JOIN author_work aw ON a.author_id = aw.author_id " +
            "GROUP BY a.author_id, a.author_nickname, a.author_url")
    List<AuthorVO> selectAuthorsWithWorkCount();
}
