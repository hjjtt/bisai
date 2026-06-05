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
    /** 多轮采样分数JSON数组, 如 [85,82,88] */
    private String sampleScores;
    /** 结构化覆盖度分析JSON */
    private String coverageDetails;
    /** 交叉模型(备用模型)评分 */
    private BigDecimal crossModelScore;
    /** 主模型与交叉模型偏差(绝对值) */
    private BigDecimal crossModelDivergence;
    /** 提交内容字数(冗长偏差修正用) */
    private Integer wordCount;
    /** 冗长偏差修正系数 */
    private BigDecimal verbosityFactor;
    @TableLogic
    private Integer deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private String indicatorName;
    @TableField(exist = false)
    private BigDecimal maxScore;
}
