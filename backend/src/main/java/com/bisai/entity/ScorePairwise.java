package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Pairwise 比较结果实体
 */
@Data
@TableName("score_pairwise")
public class ScorePairwise {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long submissionAId;
    private Long submissionBId;
    /** 比较结果: A / B / TIE */
    private String winner;
    /** AI 比较理由 */
    private String reasoning;
    /** 使用的模型 */
    private String model;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // 非持久化：用于前端展示
    @TableField(exist = false)
    private String studentAName;
    @TableField(exist = false)
    private String studentBName;
    @TableField(exist = false)
    private BigDecimal scoreA;
    @TableField(exist = false)
    private BigDecimal scoreB;
}
