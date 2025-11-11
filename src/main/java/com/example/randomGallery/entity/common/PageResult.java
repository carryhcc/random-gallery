package com.example.randomGallery.entity.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 分页结果封装类
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageResult<T> implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    /**
     * 当前页数据
     */
    private List<T> list;
    
    /**
     * 总记录数
     */
    private long total;
    
    /**
     * 当前页码
     */
    private int pageNum;
    
    /**
     * 每页大小
     */
    private int pageSize;
    
    /**
     * 总页数
     */
    private int pages;
    
    /**
     * 是否有下一页
     */
    private boolean hasNextPage;
    
    /**
     * 是否有上一页
     */
    private boolean hasPreviousPage;
    
    public PageResult(List<T> list, long total, int pageNum, int pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = Math.max(pageSize, 1); // 确保pageSize至少为1
        this.pages = (int) Math.ceil((double) total / this.pageSize);
        this.hasNextPage = pageNum < pages;
        this.hasPreviousPage = pageNum > 1;
    }
}
