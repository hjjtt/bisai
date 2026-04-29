package com.bisai.dto;

import lombok.Data;

@Data
public class PageQuery {
    private Integer page = 1;
    private Integer size = 20;
    private String sort = "created_at";
    private String order = "desc";
    private String keyword;
}
