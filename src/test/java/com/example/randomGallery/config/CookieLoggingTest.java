package com.example.randomGallery.config;

import com.example.randomGallery.entity.QO.CookieValidateQry;
import com.example.randomGallery.entity.QO.DownLoadQry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class CookieLoggingTest {
    @Test
    void cookieRequestArgumentsAreMaskedWithoutChangingRequest() {
        var aspect = new ApiLoggingAspect(new ObjectMapper());
        var validation = new CookieValidateQry();
        validation.setCookie("a1=abc123; web_session=secret-session");
        var download = new DownLoadQry("https://www.xiaohongshu.com/explore/test123");
        download.setCookie(validation.getCookie());

        String json = ReflectionTestUtils.invokeMethod(aspect, "toSafeJson", (Object) new Object[] { validation, download });

        assertNotNull(json);
        assertFalse(json.contains("secret-session"));
        assertTrue(json.contains("***"));
        assertTrue(json.contains(download.getUrl()));
        assertEquals(validation.getCookie(), download.getCookie());
    }
}
