package com.bisai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassReportDTO {
    private Long taskId;
    private String taskTitle;
    private String courseName;
    private Integer totalStudents;
    private Integer submittedCount;
    private Integer notSubmittedCount;
    private BigDecimal avgScore;
    private BigDecimal maxScore;
    private BigDecimal minScore;
    private Map<String, Integer> scoreDistribution;
    private List<StudentScoreSummary> topStudents;
    private List<StudentScoreSummary> lowStudents;
    private Map<String, BigDecimal> indicatorAvgScores;
    private List<String> commonIssues;
    private String generateTime;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentScoreSummary {
        private Long studentId;
        private String studentName;
        private BigDecimal score;
    }
}
