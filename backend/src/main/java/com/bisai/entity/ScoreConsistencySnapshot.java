package com.bisai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 评分一致性统计快照
 */
@Data
@TableName("score_consistency_snapshot")
public class ScoreConsistencySnapshot {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 任务ID, NULL 表示全局统计 */
    private Long taskId;
    private int totalEvaluated;
    private int totalTeacherConfirmed;
    /** Pearson 相关系数 */
    private BigDecimal pearsonCorrelation;
    /** Spearman 秩相关系数 */
    private BigDecimal spearmanCorrelation;
    /** 均方根误差 */
    private BigDecimal rmse;
    /** 平均绝对误差 */
    private BigDecimal mae;
    /** AI 与教师平均偏差 */
    private BigDecimal avgDivergence;
    /** 多模型一致率 */
    private BigDecimal crossModelAgreement;
    private LocalDate snapshotDate;
    private LocalDateTime createdAt;
}
