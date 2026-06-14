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

    /**
     * page 下限保护：MyBatis-Plus 分页 page 必须 >= 1，传 0 或负数会导致 offset 为负。
     */
    public Integer getPage() {
        return page != null && page >= 1 ? page : 1;
    }
}
