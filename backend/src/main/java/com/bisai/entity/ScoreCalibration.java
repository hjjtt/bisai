package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("score_calibration")
public class ScoreCalibration {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long submissionId;
    private Long indicatorId;
    private BigDecimal calibrationScore;
    private String calibrationReason;
    private String typicalAdvantages;
    private String typicalProblems;
    private String deductionBasis;
    private Long confirmedBy;
    private LocalDateTime confirmedAt;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String indicatorName;
}
