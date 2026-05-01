/*
 * Report Service
 * Author: echo
 * Date: 2026-05-01
 * Description: 报表服务类，处理报表数据组装与生成
 */

package com.bisai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bisai.dto.ClassReportDTO;
import com.bisai.dto.StudentReportDTO;
import com.bisai.entity.*;
import com.bisai.mapper.*;
import com.bisai.util.ExcelReportGenerator;
import com.bisai.util.PdfReportGenerator;
import com.bisai.util.WordReportGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final SubmissionMapper submissionMapper;
    private final CheckResultMapper checkResultMapper;
    private final ScoreResultMapper scoreResultMapper;
    private final TrainingTaskMapper trainingTaskMapper;
    private final CourseMapper courseMapper;
    private final IndicatorMapper indicatorMapper;
    private final UserMapper userMapper;

    public byte[] exportStudentReport(Long submissionId, String format) {
        Submission submission = submissionMapper.selectDetailById(submissionId);
        if (submission == null) {
            throw new RuntimeException("提交记录不存在");
        }

        List<CheckResult> checkResults = checkResultMapper.selectList(
                new LambdaQueryWrapper<CheckResult>().eq(CheckResult::getSubmissionId, submissionId)
        );

        List<ScoreResult> scoreResults = scoreResultMapper.selectList(
                new LambdaQueryWrapper<ScoreResult>().eq(ScoreResult::getSubmissionId, submissionId)
        );

        for (ScoreResult scoreResult : scoreResults) {
            if (scoreResult.getIndicatorId() != null) {
                Indicator indicator = indicatorMapper.selectById(scoreResult.getIndicatorId());
                if (indicator != null) {
                    scoreResult.setIndicatorName(indicator.getName());
                }
            }
        }

        StudentReportDTO report = StudentReportDTO.builder()
                .submissionId(submissionId)
                .studentName(submission.getStudentName())
                .className(submission.getClassName())
                .taskTitle(submission.getTaskTitle())
                .submitTime(submission.getSubmitTime())
                .version(submission.getVersion())
                .parseSummary(submission.getParseSummary())
                .parseTopics(submission.getParseTopics())
                .parseCompleteness(submission.getParseCompleteness())
                .parseQuality(submission.getParseQuality())
                .parseSuggestions(submission.getParseSuggestions())
                .totalScore(submission.getTotalScore())
                .teacherComment(submission.getTeacherComment())
                .checkResults(checkResults)
                .scoreResults(scoreResults)
                .generateTime(LocalDateTime.now())
                .build();

        try {
            return switch (format.toUpperCase()) {
                case "WORD", "DOCX" -> WordReportGenerator.generateStudentReport(report);
                case "PDF" -> PdfReportGenerator.generateStudentReport(report);
                default -> WordReportGenerator.generateStudentReport(report);
            };
        } catch (Exception e) {
            log.error("生成报告失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成报告失败: " + e.getMessage());
        }
    }

    public byte[] exportClassReport(Long taskId, String format) {
        TrainingTask task = trainingTaskMapper.selectById(taskId);
        if (task == null) {
            throw new RuntimeException("实训任务不存在");
        }

        Course course = courseMapper.selectById(task.getCourseId());

        List<Submission> submissions = submissionMapper.selectByTaskIdWithDetail(taskId);

        List<ClassReportDTO.StudentScoreSummary> allStudentScores = new ArrayList<>();
        BigDecimal totalScore = BigDecimal.ZERO;
        BigDecimal maxScore = BigDecimal.ZERO;
        BigDecimal minScore = new BigDecimal("100");
        Map<String, Integer> scoreDistribution = new LinkedHashMap<>();
        scoreDistribution.put("90-100", 0);
        scoreDistribution.put("80-89", 0);
        scoreDistribution.put("70-79", 0);
        scoreDistribution.put("60-69", 0);
        scoreDistribution.put("0-59", 0);

        for (Submission submission : submissions) {
            if (submission.getTotalScore() != null) {
                ClassReportDTO.StudentScoreSummary summary = ClassReportDTO.StudentScoreSummary.builder()
                        .studentId(submission.getStudentId())
                        .studentName(submission.getStudentName())
                        .score(submission.getTotalScore())
                        .build();
                allStudentScores.add(summary);

                totalScore = totalScore.add(submission.getTotalScore());
                if (submission.getTotalScore().compareTo(maxScore) > 0) {
                    maxScore = submission.getTotalScore();
                }
                if (submission.getTotalScore().compareTo(minScore) < 0) {
                    minScore = submission.getTotalScore();
                }

                String range = getScoreRange(submission.getTotalScore());
                scoreDistribution.put(range, scoreDistribution.getOrDefault(range, 0) + 1);
            }
        }

        List<ClassReportDTO.StudentScoreSummary> sortedScores = allStudentScores.stream()
                .sorted((a, b) -> b.getScore().compareTo(a.getScore()))
                .toList();

        List<ClassReportDTO.StudentScoreSummary> topStudents = sortedScores.stream().limit(10).toList();
        List<ClassReportDTO.StudentScoreSummary> lowStudents = sortedScores.stream()
                .sorted(Comparator.comparing(ClassReportDTO.StudentScoreSummary::getScore))
                .limit(10).toList();

        BigDecimal avgScore = allStudentScores.isEmpty() ? BigDecimal.ZERO :
                totalScore.divide(new BigDecimal(allStudentScores.size()), 2, RoundingMode.HALF_UP);

        ClassReportDTO report = ClassReportDTO.builder()
                .taskId(taskId)
                .taskTitle(task.getTitle())
                .courseName(course != null ? course.getName() : "")
                .totalStudents(getTotalStudents())
                .submittedCount(submissions.size())
                .notSubmittedCount(getTotalStudents() - submissions.size())
                .avgScore(avgScore)
                .maxScore(maxScore)
                .minScore(minScore)
                .scoreDistribution(scoreDistribution)
                .topStudents(topStudents)
                .lowStudents(lowStudents)
                .commonIssues(Collections.emptyList())
                .generateTime(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .build();

        try {
            return switch (format.toUpperCase()) {
                case "EXCEL", "XLSX" -> ExcelReportGenerator.generateClassReport(report);
                case "PDF" -> PdfReportGenerator.generateClassReport(report);
                default -> ExcelReportGenerator.generateClassReport(report);
            };
        } catch (Exception e) {
            log.error("生成班级报告失败: {}", e.getMessage(), e);
            throw new RuntimeException("生成班级报告失败: " + e.getMessage());
        }
    }

    private String getScoreRange(BigDecimal score) {
        int s = score.intValue();
        if (s >= 90) return "90-100";
        if (s >= 80) return "80-89";
        if (s >= 70) return "70-79";
        if (s >= 60) return "60-69";
        return "0-59";
    }

    private int getTotalStudents() {
        return Math.toIntExact(userMapper.selectCount(
                new LambdaQueryWrapper<User>().eq(User::getRole, "STUDENT")));
    }
}
