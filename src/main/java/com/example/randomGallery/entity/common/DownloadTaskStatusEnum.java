package com.example.randomGallery.entity.common;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

/**
 * 下载任务状态枚举（对应 xhs_download_task 表的 status 字段）
 */
@Getter
public enum DownloadTaskStatusEnum {

    /**
     * 等待中
     */
    WAITING(0, "等待中"),

    /**
     * 已完成
     */
    COMPLETED(1, "已完成"),

    /**
     * 失败
     */
    FAILED(2, "失败");

    @EnumValue
    private final int value;

    private final String label;

    DownloadTaskStatusEnum(int value, String label) {
        this.value = value;
        this.label = label;
    }

    /**
     * 从数据库值转换为枚举
     */
    public static DownloadTaskStatusEnum fromValue(Integer value) {
        if (value == null) {
            return null;
        }
        for (DownloadTaskStatusEnum status : DownloadTaskStatusEnum.values()) {
            if (status.getValue() == value) {
                return status;
            }
        }
        throw new IllegalArgumentException("不支持的任务状态：" + value);
    }
}
