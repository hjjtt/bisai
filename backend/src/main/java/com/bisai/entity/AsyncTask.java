package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("async_task")
public class AsyncTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskType;
    private Long bizId;
    private String status;
    private Integer progress; // 进度百分比 0-100
    private String currentStep; // 当前执行步骤描述
    private Integer retryCount;
    private Integer maxRetry;
    private LocalDateTime nextRunAt;
    private String errorMessage;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
