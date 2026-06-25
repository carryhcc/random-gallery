package com.example.randomGallery.service;

import com.example.randomGallery.service.Impl.VisitServiceImpl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class VisitServiceImplTest {

    @Test
    void isPrivateIp_shouldReturnTrue_forLoopback() {
        assertTrue(VisitServiceImpl.isPrivateIp("127.0.0.1"));
    }

    @Test
    void isPrivateIp_shouldReturnTrue_forPrivateClass10() {
        assertTrue(VisitServiceImpl.isPrivateIp("10.0.0.1"));
    }

    @Test
    void isPrivateIp_shouldReturnTrue_forPrivateClass192() {
        assertTrue(VisitServiceImpl.isPrivateIp("192.168.1.100"));
    }

    @Test
    void isPrivateIp_shouldReturnFalse_forPublicIp() {
        assertFalse(VisitServiceImpl.isPrivateIp("8.8.8.8"));
    }

    @Test
    void extractFirstIp_shouldReturnFirst_whenCommaSeparated() {
        String result = VisitServiceImpl.extractFirstIp("1.2.3.4, 10.0.0.1, 192.168.0.1");
        assertEquals("1.2.3.4", result);
    }

    @Test
    void extractFirstIp_shouldReturnSelf_whenSingleIp() {
        String result = VisitServiceImpl.extractFirstIp("8.8.8.8");
        assertEquals("8.8.8.8", result);
    }

    @Test
    void extractFirstIp_shouldReturnNull_whenNull() {
        assertNull(VisitServiceImpl.extractFirstIp(null));
    }
}
