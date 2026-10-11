package com.example.randomGallery.service.Impl;

import com.example.randomGallery.service.DownloadTaskService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CookieValidationRequestTest {
    private DownloadApiImpl service() {
        return new DownloadApiImpl(mock(DownloadTaskService.class), mock(DownloadTaskConsumer.class));
    }

    @Test
    void rejectsMalformedCookieBeforeCallingDownloader() {
        var service = service();
        assertEquals("Cookie 为空", service.validateCookie(null).getMessage());
        assertEquals("Cookie 格式不正确", service.validateCookie("a1=abc123; missing_pair").getMessage());
        assertEquals("Cookie 格式不正确", service.validateCookie("a1=abc123; web_session=xyz\r\n789").getMessage());
        assertEquals("未配置 other.downloader.probe-url", service.validateCookie("a1=abc123; web_session=xyz789").getMessage());
    }

    @Test
    void probeUsesCookieWithoutDownloadingOrCreatingTask() throws Exception {
        var tasks = mock(DownloadTaskService.class);
        var consumer = mock(DownloadTaskConsumer.class);
        var service = new DownloadApiImpl(tasks, consumer);
        var request = new AtomicReference<String>();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/xhs/detail", exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] body = "{\"data\":{\"作品ID\":\"test123\"}}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "xhsDetailUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/xhs/detail");
            ReflectionTestUtils.setField(service, "probeUrl", "https://www.xiaohongshu.com/explore/test123");
            assertEquals(Boolean.TRUE, service.validateCookie("a1=abc123; web_session=xyz789").getData());
            assertTrue(request.get().contains("\"download\":false"));
            assertTrue(request.get().contains("a1=abc123; web_session=xyz789"));
            verifyNoInteractions(tasks, consumer);
        } finally {
            server.stop(0);
        }
    }

    @Test
    void httpFailureIsServiceErrorRatherThanInvalidCookie() throws Exception {
        var service = service();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/xhs/detail", exchange -> {
            exchange.sendResponseHeaders(503, -1);
            exchange.close();
        });
        server.start();
        try {
            ReflectionTestUtils.setField(service, "xhsDetailUrl", "http://127.0.0.1:" + server.getAddress().getPort() + "/xhs/detail");
            ReflectionTestUtils.setField(service, "probeUrl", "https://www.xiaohongshu.com/explore/test123");
            var result = service.validateCookie("a1=abc123; web_session=xyz789");
            assertNotEquals(200, result.getCode());
            assertNull(result.getData());
        } finally {
            server.stop(0);
        }
    }
}
