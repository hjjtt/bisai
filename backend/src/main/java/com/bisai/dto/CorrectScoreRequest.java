package com.bisai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CorrectScoreRequest {
    /** 指标 ID，为空时修正总分 */
    private Long indicatorId;
    @NotNull(message = "新分数不能为空")
    private BigDecimal newScore;
    @NotBlank(message = "修正原因不能为空")
    private String reason;
}
