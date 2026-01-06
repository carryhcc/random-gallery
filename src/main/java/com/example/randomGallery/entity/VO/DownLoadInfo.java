package com.example.randomGallery.entity.VO;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * 作品数据响应外层对象
 */
@Data
public class DownLoadInfo {
    // 响应消息
    @JsonProperty("message")
    private String message;

    // 请求参数
    @JsonProperty("params")
    private Params params;

    // 核心数据
    @JsonProperty("data")
    private Data data;

    /**
     * 请求参数内部类
     */
    @lombok.Data
    public static class Params {
        // 作品链接
        @JsonProperty("url")
        private String url;

        // 是否下载
        @JsonProperty("download")
        private boolean download;

        // 索引（可为null，用String接收）
        @JsonProperty("index")
        private String index;

        // Cookie（可为null）
        @JsonProperty("cookie")
        private String cookie;

        // 代理（可为null）
        @JsonProperty("proxy")
        private String proxy;

        // 是否跳过
        @JsonProperty("skip")
        private boolean skip;

    }

    /**
     * 作品核心数据内部类
     */
    @lombok.Data
    public static class Data {
        // 收藏数量
        @JsonProperty("收藏数量")
        private String collectCount;

        // 评论数量
        @JsonProperty("评论数量")
        private String commentCount;

        // 分享数量
        @JsonProperty("分享数量")
        private String shareCount;

        // 点赞数量
        @JsonProperty("点赞数量")
        private String likeCount;

        // 作品标签
        @JsonProperty("作品标签")
        private String workTags;

        // 作品ID
        @JsonProperty("作品ID")
        private String workId;

        // 作品链接
        @JsonProperty("作品链接")
        private String workUrl;

        // 作品标题
        @JsonProperty("作品标题")
        private String workTitle;

        // 作品描述
        @JsonProperty("作品描述")
        private String workDescription;

        // 作品类型
        @JsonProperty("作品类型")
        private String workType;

        // 发布时间
        @JsonProperty("发布时间")
        private String publishTime;

        // 最后更新时间
        @JsonProperty("最后更新时间")
        private String lastUpdateTime;

        // 时间戳
        @JsonProperty("时间戳")
        private double timestamp;

        // 作者昵称
        @JsonProperty("作者昵称")
        private String authorNickname;

        // 作者ID
        @JsonProperty("作者ID")
        private String authorId;

        // 作者链接
        @JsonProperty("作者链接")
        private String authorUrl;

        // 下载地址列表（支持null元素）
        @JsonProperty("下载地址")
        private List<String> downloadUrls;

        // 动图地址列表（包含null）
        @JsonProperty("动图地址")
        private List<String> gifUrls;
    }
}