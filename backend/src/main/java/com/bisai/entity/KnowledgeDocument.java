package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("knowledge_document")
public class KnowledgeDocument {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long knowledgeBaseId;
    private Long fileId;
    private String originalName;
    private String parseStatus;
    private String vectorStatus;
    private Boolean enabled;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 非数据库字段 - 前端展示用
    @TableField(exist = false)
    private String name;
    @TableField(exist = false)
    private String courseName;
    @TableField(exist = false)
    private Boolean vectorized;
    @TableField(exist = false)
    private LocalDateTime updateTime;
}
