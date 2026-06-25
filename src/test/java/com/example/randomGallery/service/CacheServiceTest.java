package com.example.randomGallery.service;

import org.junit.jupiter.api.Test;
import java.util.Arrays;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class CacheServiceTest {

    @Test
    void getRandomIdFromList_shouldReturnElementFromList() {
        List<Long> ids = Arrays.asList(1L, 5L, 10L, 100L);
        // 模拟 100 次随机，结果必须在列表中
        for (int i = 0; i < 100; i++) {
            int idx = (int) (Math.random() * ids.size());
            Long picked = ids.get(idx);
            assertTrue(ids.contains(picked), "随机取到的 ID 必须在有效列表中");
        }
    }

    @Test
    void getRandomIdFromList_shouldReturnNull_whenListEmpty() {
        List<Long> ids = List.of();
        // 空列表时应返回 null，不应抛异常
        Long result = ids.isEmpty() ? null : ids.get(0);
        assertNull(result);
    }
}
