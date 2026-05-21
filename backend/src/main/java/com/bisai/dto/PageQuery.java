package com.bisai.dto;

import lombok.Data;

@Data
public class PageQuery {
    private Integer page = 1;
    private Integer size = 20;
    private String keyword;

    private static final int MAX_SIZE = 100;

    public Integer getSize() {
        return size != null ? Math.min(size, MAX_SIZE) : 20;
    }
}
