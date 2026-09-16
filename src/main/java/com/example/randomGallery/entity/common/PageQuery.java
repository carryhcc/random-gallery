package com.example.randomGallery.entity.common;

import lombok.Getter;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;

public abstract class PageQuery implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    public static final String ASC = "ASC";
    public static final String DESC = "DESC";
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * 单页上限。真实调用方最多用 10（web 6/10、Android 10），这里留足余量；
     * 不封顶时 pageSize 可被放大成一次拉回整表。
     */
    public static final int MAX_PAGE_SIZE = 200;

    /**
     * 偏移量上限。LIMIT offset,N 在 84 万行的表上实测 1 万→60ms、10 万→308ms、80 万→5091ms，
     * 所以按 offset 封顶，而不是按页号。
     */
    public static final int MAX_OFFSET = 100_000;

    private int pageSize = 10;
    private int pageIndex = 1;
    @Getter
    private String orderBy;
    @Getter
    private String orderDirection = "DESC";
    @Setter
    @Getter
    private String groupBy;
    @Setter
    @Getter
    private boolean needTotalCount = true;

    public int getPageIndex() {
        int index = Math.max(this.pageIndex, 1);

        // 与单页大小联动封顶，保证 (pageIndex - 1) * pageSize 不超过 MAX_OFFSET
        return Math.min(index, Math.max(MAX_OFFSET / getPageSize(), 1));
    }

    public PageQuery setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
        return this;
    }

    public int getPageSize() {
        if (this.pageSize < 1) {
            this.pageSize = DEFAULT_PAGE_SIZE;
        }

        return Math.min(this.pageSize, MAX_PAGE_SIZE);
    }

    public PageQuery setPageSize(int pageSize) {
        if (pageSize < 1) {
            pageSize = DEFAULT_PAGE_SIZE;
        }

        this.pageSize = pageSize;
        return this;
    }

    public int getOffset() {
        return (this.getPageIndex() - 1) * this.getPageSize();
    }

    public PageQuery setOrderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public PageQuery setOrderDirection(String orderDirection) {
        if ("ASC".equalsIgnoreCase(orderDirection) || "DESC".equalsIgnoreCase(orderDirection)) {
            this.orderDirection = orderDirection;
        }

        return this;
    }
}