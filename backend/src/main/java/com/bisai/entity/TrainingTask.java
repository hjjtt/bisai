package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("training_task")
public class TrainingTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long templateId;
    private String title;
    private String requirements;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean allowResubmit;
    private String allowedFileTypes;
    private Long maxFileSize;
    private String status;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String courseName;
    @TableField(exist = false)
    private String templateName;
}
