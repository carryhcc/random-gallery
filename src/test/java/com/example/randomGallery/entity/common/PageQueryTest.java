package com.example.randomGallery.entity.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 分页边界：pageSize 与 offset 都必须被封顶，否则 GET /api/pic/list 可被放大成深翻页全表扫。
 */
class PageQueryTest {

    private static class Q extends PageQuery {
    }

    private static Q page(int pageSize, int pageIndex) {
        Q q = new Q();
        q.setPageSize(pageSize);
        q.setPageIndex(pageIndex);
        return q;
    }

    @Test
    void pageSize_shouldStayUnchanged_whenWithinLimit() {
        assertEquals(6, new Q().setPageSize(6).getPageSize());
        assertEquals(PageQuery.MAX_PAGE_SIZE, new Q().setPageSize(PageQuery.MAX_PAGE_SIZE).getPageSize());
    }

    @Test
    void pageSize_shouldBeClamped_whenTooLarge() {
        assertEquals(PageQuery.MAX_PAGE_SIZE, new Q().setPageSize(1_000_000).getPageSize());
    }

    @Test
    void pageSize_shouldFallBackToDefault_whenNotPositive() {
        assertEquals(10, new Q().setPageSize(0).getPageSize());
        assertEquals(10, new Q().setPageSize(-5).getPageSize());
    }

    @Test
    void pageIndex_shouldStayUnchanged_whenInsideLimit() {
        assertEquals(1, new Q().setPageSize(10).setPageIndex(1).getPageIndex());
        assertEquals(1000, new Q().setPageSize(10).setPageIndex(1000).getPageIndex());
    }

    @Test
    void offset_shouldNeverExceedMaxOffset() {
        for (int size : new int[]{1, 6, 10, 20, 200, 1_000_000}) {
            for (int index : new int[]{1, 500, 50_000, 2_000_000, Integer.MAX_VALUE}) {
                Q q = page(size, index);
                assertTrue(q.getPageIndex() >= 1, "页码至少为 1");
                assertTrue(q.getOffset() <= PageQuery.MAX_OFFSET,
                        "size=" + size + " index=" + index + " -> offset=" + q.getOffset());
            }
        }
    }
}
