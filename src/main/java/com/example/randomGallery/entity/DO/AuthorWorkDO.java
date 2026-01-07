package com.example.randomGallery.entity.DO;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 作者作品关联DO（对应数据库表：author_work）
 */
@Data
@TableName(value = "author_work")
public class AuthorWorkDO {

    /**
     * 主键ID（自增）
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 作者ID
     */
    @TableField(value = "author_id")
    private String authorId;

    /**
     * 作品ID
     */
    @TableField(value = "work_id")
    private String workId;

    /**
     * 创建时间
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
