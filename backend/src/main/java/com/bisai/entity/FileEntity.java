package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("file")
public class FileEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long knowledgeDocumentId;
    private String originalName;
    private String filePath;
    private String fileType;
    private Long fileSize;
    private String fileHash;
    private Integer version;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
