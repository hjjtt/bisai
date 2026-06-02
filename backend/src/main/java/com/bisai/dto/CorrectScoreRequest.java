package com.bisai.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class CorrectScoreRequest {
    private Long indicatorId;
    private BigDecimal newScore;
    private String reason;
}
