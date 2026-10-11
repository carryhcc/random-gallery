package com.example.randomGallery.service.Impl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cookie 校验的探测响应判定：能解析出 data 且作品ID 非空才算有效，
 * data 缺失、作品ID 空白、响应体无法解析都视为 Cookie 失效。
 */
class CookieValidationTest {

    @Test
    void validWhenDataHasWorkId() {
        String body = """
                {"message":"success","params":{"url":"https://www.xiaohongshu.com/explore/abc"},"data":{"作品ID":"abc123","作品标题":"测试笔记"}}
                """;
        assertTrue(DownloadApiImpl.isValidProbeResponse(body));
    }

    @Test
    void invalidWhenDataIsNull() {
        String body = "{\"message\":\"cookie 已失效\",\"data\":null}";
        assertFalse(DownloadApiImpl.isValidProbeResponse(body));
    }

    @Test
    void invalidWhenWorkIdBlank() {
        String body = "{\"data\":{\"作品ID\":\"  \"}}";
        assertFalse(DownloadApiImpl.isValidProbeResponse(body));
    }

    @Test
    void invalidWhenMalformedJson() {
        assertFalse(DownloadApiImpl.isValidProbeResponse("not a json"));
    }
}
