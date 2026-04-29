package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("document_chunk")
public class DocumentChunk {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long knowledgeDocumentId;
    private Integer chunkIndex;
    private String content;
    private Integer tokenCount;
    private String embedding;
    private LocalDateTime createdAt;
}
