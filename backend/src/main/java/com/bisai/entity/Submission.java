package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("submission")
public class Submission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long studentId;
    private LocalDateTime submitTime;
    private Integer version;
    private String parseStatus;
    private String checkStatus;
    private String scoreStatus;
    private BigDecimal totalScore;
    private BigDecimal autoTotalScore;
    private String teacherComment;
    private String parseSummary;
    private String parseTopics;
    private String parseCompleteness;
    private String parseQuality;
    private String parseSuggestions;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String studentName;
    @TableField(exist = false)
    private String taskTitle;
}
