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
        return Math.max(this.pageIndex, 1);
    }

    public PageQuery setPageIndex(int pageIndex) {
        this.pageIndex = pageIndex;
        return this;
    }

    public int getPageSize() {
        if (this.pageSize < 1) {
            this.pageSize = 10;
        }

        return this.pageSize;
    }

    public PageQuery setPageSize(int pageSize) {
        if (pageSize < 1) {
            pageSize = 10;
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