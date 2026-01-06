package com.example.randomGallery.entity.common;


import lombok.Getter;

/**
 * 媒体类型枚举（对应xhs_work_media表的media_type字段）
 */
@Getter
public enum MediaTypeEnum {
    /**
     * 图片地址（对应JSON中的下载地址）
     */
    IMAGE("image"),
    /**
     * 动图/视频地址（对应JSON中的动图地址）
     */
    GIF("gif");

    private final String value;

    MediaTypeEnum(String value) {
        this.value = value;
    }

    // 用于从数据库值转换为枚举
    public static MediaTypeEnum fromValue(String value) {
        for (MediaTypeEnum type : MediaTypeEnum.values()) {
            if (type.getValue().equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的媒体类型：" + value);
    }
}