package com.example.randomGallery.entity.QO;

import lombok.Data;
import lombok.NonNull;

import java.util.List;

/**
 * 图片下载查询参数
 */
@Data
public class DownLoadQry {
    /**
     * 作品链接，自动提取，不支持多链接；必需参数	无
     */
    @NonNull
    private String url;
    /**
     * 是否下载作品文件；设置为 true 将会耗费更多时间；可选参数	false
     */
    private Boolean download;
    /**
     * 下载指定序号的图片文件，仅对图文作品生效；download 参数设置为 false 时不生效；可选参数	null
     */
    private List<Integer> index;
    /**
     * 请求数据时使用的 Cookie；可选参数	配置文件 cookie 参数
     */
    private String cookie;
    /**
     * 请求数据时使用的代理；可选参数	配置文件 proxy 参数
     */
    private String proxy;
    /**
     * 是否跳过存在下载记录的作品；设置为 true 将不会返回存在下载记录的作品数据；可选参数	false
     */
    private Boolean skip;
}