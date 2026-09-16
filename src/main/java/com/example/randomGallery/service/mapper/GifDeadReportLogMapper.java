package com.example.randomGallery.service.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.randomGallery.entity.DO.GifDeadReportLogDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Map;

/**
 * GIF 死链上报日志 Mapper
 */
@Mapper
public interface GifDeadReportLogMapper extends BaseMapper<GifDeadReportLogDO> {

    /**
     * 统计死链上报情况：总数、今日上报数、本周上报数
     */
    @Select("""
        SELECT
            COUNT(*) AS total,
            SUM(CASE WHEN DATE(create_time) = CURDATE() THEN 1 ELSE 0 END) AS today,
            SUM(CASE WHEN YEARWEEK(create_time, 1) = YEARWEEK(CURDATE(), 1) THEN 1 ELSE 0 END) AS thisWeek,
            COUNT(DISTINCT media_id) AS uniqueMedia
        FROM gif_dead_report_log
    """)
    Map<String, Object> getReportStats();
}
