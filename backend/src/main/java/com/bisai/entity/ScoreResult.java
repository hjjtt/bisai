package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("score_result")
public class ScoreResult {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long indicatorId;
    private BigDecimal autoScore;
    private BigDecimal teacherScore;
    private BigDecimal finalScore;
    private String reason;
    private String evidence;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String indicatorName;
    @TableField(exist = false)
    private BigDecimal maxScore;
}
