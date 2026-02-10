package com.example.randomGallery.entity.VO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 图片数据封装类（用于图片下载）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImageData {
    private int index;
    private byte[] bytes;
}