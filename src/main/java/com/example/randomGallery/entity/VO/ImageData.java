package com.example.randomGallery.entity.VO;

public class ImageData {
    public int index;
    public byte[] bytes;

    public ImageData(int index, byte[] bytes) {
        this.index = index;
        this.bytes = bytes;
    }
}