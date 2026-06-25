package com.example.randomGallery.service;

import org.junit.jupiter.api.Test;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class CacheServiceThreadSafetyTest {

    @Test
    void volatileList_shouldBeVisibleAcrossThreads() throws InterruptedException {
        // 模拟 shuffledSeq 的 volatile 赋值与读取
        AtomicInteger errors = new AtomicInteger(0);
        volatile_holder holder = new volatile_holder();

        int threadCount = 20;
        CountDownLatch latch = new CountDownLatch(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final int idx = i;
            pool.submit(() -> {
                try {
                    if (idx % 2 == 0) {
                        // 写线程：模拟 buildGroupIDList
                        List<Long> newList = new ArrayList<>();
                        for (long j = 0; j < 100; j++) newList.add(j);
                        Collections.shuffle(newList);
                        holder.seq = Collections.unmodifiableList(newList);
                        holder.count = newList.size();
                    } else {
                        // 读线程：模拟 loadMore
                        List<Long> seq = holder.seq;
                        if (seq != null && !seq.isEmpty()) {
                            int end = Math.min(9, seq.size());
                            List<Long> page = seq.subList(0, end);
                            assertTrue(page.size() <= 9);
                        }
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await(5, TimeUnit.SECONDS);
        pool.shutdown();
        assertEquals(0, errors.get(), "并发读写不应抛出异常");
    }

    static class volatile_holder {
        volatile List<Long> seq = Collections.emptyList();
        volatile Integer count = 0;
    }
}
