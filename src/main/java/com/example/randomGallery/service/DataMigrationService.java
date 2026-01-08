package com.example.randomGallery.service;

/**
 * 数据迁移服务
 * 用于处理历史数据的迁移，特别是标签数据的迁移
 */
public interface DataMigrationService {

    /**
     * 迁移历史标签数据
     * 从xhs_work_base表的work_tags字段提取标签，并创建关联
     */
    void migrateData();

    /**
     * 获取迁移进度信息
     * 
     * @return 迁移信息字符串
     */
    String getMigrationInfo();
}
