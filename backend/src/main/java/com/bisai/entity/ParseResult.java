package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("parse_result")
public class ParseResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long knowledgeDocumentId;
    private Long fileId;
    private String parserType;
    private String content;
    private String summary;
    private String mainTopics;
    private String completeness;
    private String quality;
    private String suggestions;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
