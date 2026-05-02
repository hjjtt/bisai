package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("score_correction")
public class ScoreCorrection {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long submissionId;
    private Long indicatorId;
    private BigDecimal originalScore;
    private BigDecimal newScore;
    private String reason;
    private Long correctedBy;
    private LocalDateTime correctedAt;

    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
}
