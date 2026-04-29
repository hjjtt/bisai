package com.bisai.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志返回 DTO，避免直接暴露实体内部字段
 */
@Data
public class OperationLogDTO {
    private Long id;
    private String username;
    private String action;
    private String description;
    private String ip;
    private String requestPath;
    private String requestMethod;
    private LocalDateTime createdAt;
}
