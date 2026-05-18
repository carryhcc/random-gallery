package com.example.randomGallery.service;

import cn.hutool.core.util.StrUtil;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * 统一处理前端图片代理地址改写规则。
 */
public final class ImageProxyUrlResolver {

    private static final String TARGET_HOST = "telegra.ph";
    private static final String TARGET_SCHEME = "https";

    private ImageProxyUrlResolver() {
    }

    public static String resolve(String imageUrl, boolean proxyEnabled) {
        if (!proxyEnabled || !shouldProxy(imageUrl)) {
            return imageUrl;
        }
        return "/api/image/proxy?url=" + URLEncoder.encode(imageUrl, StandardCharsets.UTF_8);
    }

    public static boolean shouldProxy(String imageUrl) {
        if (StrUtil.isBlank(imageUrl)) {
            return false;
        }

        try {
            URI uri = URI.create(imageUrl.trim());
            return TARGET_SCHEME.equalsIgnoreCase(uri.getScheme())
                    && TARGET_HOST.equalsIgnoreCase(uri.getHost());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
