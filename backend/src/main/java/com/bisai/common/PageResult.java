package com.bisai.common;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {
    private List<T> items;
    private long page;
    private long size;
    private long total;

    public PageResult(List<T> items, long page, long size, long total) {
        this.items = items;
        this.page = page;
        this.size = size;
        this.total = total;
    }
}
