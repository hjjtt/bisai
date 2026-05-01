package com.bisai.dto;

import com.bisai.entity.CheckResult;
import com.bisai.entity.ScoreResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentReportDTO {
    private Long submissionId;
    private String studentName;
    private String className;
    private String taskTitle;
    private LocalDateTime submitTime;
    private Integer version;
    private String parseSummary;
    private String parseTopics;
    private String parseCompleteness;
    private String parseQuality;
    private String parseSuggestions;
    private BigDecimal totalScore;
    private String teacherComment;
    private List<CheckResult> checkResults;
    private List<ScoreResult> scoreResults;
    private LocalDateTime generateTime;
}
