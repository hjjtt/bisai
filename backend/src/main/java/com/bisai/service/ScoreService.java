package com.bisai.service;

import com.bisai.common.Result;
import com.bisai.entity.*;
import com.bisai.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreService {

    private final SubmissionMapper submissionMapper;
    private final ScoreResultMapper scoreResultMapper;
    private final CheckResultMapper checkResultMapper;
    private final TrainingTaskMapper taskMapper;
    private final IndicatorMapper indicatorMapper;
    private final MessageService messageService;
    private final FileMapper fileMapper;
    private final ParseResultMapper parseResultMapper;
    private final AsyncTaskService asyncTaskService;
    private final ScoreCorrectionMapper scoreCorrectionMapper;

    /**
     * 触发智能解析
     */
    public Result<Void> triggerParse(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }
        
        // 检查是否已有任务在运行
        if (isTaskRunning("PARSE", submissionId)) {
            return Result.error("解析任务正在处理中，请勿重复操作");
        }

        try {
            submission.setParseStatus("PARSING");
            submissionMapper.updateById(submission);
            asyncTaskService.createTask("PARSE", submissionId);
            return Result.ok();
        } catch (Exception e) {
            log.error("触发解析失败: {}", e.getMessage());
            return Result.error("解析失败: " + e.getMessage());
        }
    }

    /**
     * 触发智能核查
     */
    public Result<Void> triggerCheck(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }

        // 检查是否已有任务在运行
        if (isTaskRunning("CHECK", submissionId)) {
            return Result.error("核查任务正在处理中，请勿重复操作");
        }

        try {
            submission.setCheckStatus("CHECKING");
            submissionMapper.updateById(submission);
            asyncTaskService.createTask("CHECK", submissionId);
            return Result.ok();
        } catch (Exception e) {
            log.error("触发核查失败: {}", e.getMessage());
            return Result.error("核查失败: " + e.getMessage());
        }
    }

    /**
     * 触发智能评分
     */
    public Result<Void> triggerScore(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }

        // 检查是否已有任务在运行
        if (isTaskRunning("SCORE", submissionId)) {
            return Result.error("评分任务正在处理中，请勿重复操作");
        }

        try {
            submission.setScoreStatus("SCORING");
            submissionMapper.updateById(submission);
            asyncTaskService.createTask("SCORE", submissionId);
            return Result.ok();
        } catch (Exception e) {
            log.error("触发评分失败: {}", e.getMessage());
            return Result.error("评分失败: " + e.getMessage());
        }
    }

    /**
     * 检查任务是否正在运行
     */
    private boolean isTaskRunning(String taskType, Long bizId) {
        List<AsyncTask> tasks = asyncTaskService.getTasksByBizId(bizId);
        return tasks.stream().anyMatch(t -> 
            taskType.equals(t.getTaskType()) && 
            ( "PENDING".equals(t.getStatus()) || "RUNNING".equals(t.getStatus()) || "RETRYING".equals(t.getStatus()) )
        );
    }

    public Result<List<CheckResult>> getCheckResults(Long submissionId) {
        List<CheckResult> results = checkResultMapper.selectList(
                new LambdaQueryWrapper<CheckResult>().eq(CheckResult::getSubmissionId, submissionId)
        );
        return Result.ok(results);
    }

    public Result<List<ScoreResult>> getScoreResults(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }

        // 获取已有评分结果
        List<ScoreResult> results = scoreResultMapper.selectList(
                new LambdaQueryWrapper<ScoreResult>().eq(ScoreResult::getSubmissionId, submissionId)
        );

        // 获取任务关联的评分指标
        TrainingTask task = taskMapper.selectById(submission.getTaskId());
        List<Indicator> indicators = List.of();
        if (task != null && task.getTemplateId() != null) {
            indicators = indicatorMapper.selectList(
                    new LambdaQueryWrapper<Indicator>()
                            .eq(Indicator::getTemplateId, task.getTemplateId())
                            .isNull(Indicator::getParentId)
                            .orderByAsc(Indicator::getSortOrder)
            );
        }

        // 如果没有评分结果，则根据指标模板返回初始化列表
        if (results.isEmpty()) {
            List<ScoreResult> initialResults = indicators.stream().map(ind -> {
                ScoreResult sr = new ScoreResult();
                sr.setSubmissionId(submissionId);
                sr.setIndicatorId(ind.getId());
                sr.setIndicatorName(ind.getName());
                sr.setMaxScore(ind.getMaxScore());
                sr.setCreatedAt(LocalDateTime.now());
                sr.setUpdatedAt(LocalDateTime.now());
                return sr;
            }).collect(java.util.stream.Collectors.toList());
            return Result.ok(initialResults);
        }

        // 补齐已有结果的指标名称和最高分
        Map<Long, Indicator> indMap = indicators.stream()
                .collect(java.util.stream.Collectors.toMap(Indicator::getId, i -> i, (a, b) -> a));
        results.forEach(r -> {
            Indicator ind = indMap.get(r.getIndicatorId());
            if (ind != null) {
                r.setIndicatorName(ind.getName());
                r.setMaxScore(ind.getMaxScore());
            }
        });

        return Result.ok(results);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> saveTeacherScores(Long submissionId, List<ScoreResult> scores, String comment) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }

        // 获取任务评分模板的所有指标（用于权重计算）
        TrainingTask task = taskMapper.selectById(submission.getTaskId());
        List<Indicator> allIndicators = List.of();
        if (task != null && task.getTemplateId() != null) {
            allIndicators = indicatorMapper.selectList(
                    new LambdaQueryWrapper<Indicator>()
                            .eq(Indicator::getTemplateId, task.getTemplateId())
                            .isNull(Indicator::getParentId)
            );
        }
        final Map<Long, Indicator> indicatorMap = allIndicators.stream()
                .collect(java.util.stream.Collectors.toMap(Indicator::getId, ind -> ind));

        BigDecimal autoTotalScore = BigDecimal.ZERO;
        BigDecimal totalScore = BigDecimal.ZERO;

        for (ScoreResult score : scores) {
            score.setSubmissionId(submissionId);
            score.setFinalScore(score.getTeacherScore());

            ScoreResult existing = scoreResultMapper.selectOne(
                    new LambdaQueryWrapper<ScoreResult>()
                            .eq(ScoreResult::getSubmissionId, submissionId)
                            .eq(ScoreResult::getIndicatorId, score.getIndicatorId())
            );
            if (existing != null) {
                score.setId(existing.getId());
                // 保留原有的autoScore（如果新score没有设置）
                if (score.getAutoScore() == null) {
                    score.setAutoScore(existing.getAutoScore());
                }
                scoreResultMapper.updateById(score);
            } else {
                scoreResultMapper.insert(score);
            }

            // 计算AI建议总分（优化后的占比权重算法）
            if (score.getAutoScore() != null) {
                Indicator ind = indicatorMap.get(score.getIndicatorId());
                if (ind != null && ind.getWeight() != null && ind.getMaxScore() != null && ind.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
                    // 公式：(得分 / 满分) * 权重
                    BigDecimal contribution = score.getAutoScore()
                            .divide(ind.getMaxScore(), 4, RoundingMode.HALF_UP)
                            .multiply(ind.getWeight());
                    autoTotalScore = autoTotalScore.add(contribution);
                } else {
                    autoTotalScore = autoTotalScore.add(score.getAutoScore());
                }
            }

            // 计算教师确认总分（优化后的占比权重算法）
            if (score.getTeacherScore() != null) {
                Indicator ind = indicatorMap.get(score.getIndicatorId());
                if (ind != null && ind.getWeight() != null && ind.getMaxScore() != null && ind.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal contribution = score.getTeacherScore()
                            .divide(ind.getMaxScore(), 4, RoundingMode.HALF_UP)
                            .multiply(ind.getWeight());
                    totalScore = totalScore.add(contribution);
                } else {
                    totalScore = totalScore.add(score.getTeacherScore());
                }
            }
        }

        submission.setTeacherComment(comment);
        submission.setAutoTotalScore(autoTotalScore.setScale(2, RoundingMode.HALF_UP));
        submission.setTotalScore(totalScore.setScale(2, RoundingMode.HALF_UP));
        submission.setScoreStatus("TEACHER_CONFIRMED");
        submissionMapper.updateById(submission);

        return Result.ok();
    }

    public Result<Void> publishScore(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }
        submission.setScoreStatus("PUBLISHED");
        submissionMapper.updateById(submission);

        // 发送消息通知学生成绩已发布
        try {
            messageService.sendMessage(
                    submission.getStudentId(),
                    "SCORE_PUBLISHED",
                    "实训成绩已发布",
                    String.format("您的实训任务（提交ID:%d）成绩已发布，最终得分：%.2f分，请及时查看。",
                            submissionId, submission.getTotalScore() != null ? submission.getTotalScore().doubleValue() : 0),
                    submissionId
            );
        } catch (Exception e) {
            log.warn("发送成绩发布消息失败: {}", e.getMessage());
        }

        return Result.ok();
    }

    public Result<Void> returnSubmission(Long submissionId, String reason) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }
        submission.setScoreStatus("RETURNED");
        submissionMapper.updateById(submission);
        return Result.ok();
    }

    /**
     * 客观评分 - 基于明确规则自动计算
     */
    public Result<Map<String, Object>> calculateObjectiveScore(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }

        TrainingTask task = taskMapper.selectById(submission.getTaskId());
        if (task == null) {
            return Result.error(40401, "任务不存在");
        }

        List<FileEntity> files = fileMapper.selectList(
                new LambdaQueryWrapper<FileEntity>().eq(FileEntity::getSubmissionId, submissionId)
        );

        ParseResult parseResult = parseResultMapper.selectOne(
                new LambdaQueryWrapper<ParseResult>()
                        .eq(ParseResult::getSubmissionId, submissionId)
                        .last("LIMIT 1")
        );
        String content = parseResult != null ? parseResult.getContent() : "";

        // 规则评分
        ObjectiveScoreResult objective = new ObjectiveScoreResult();

        // 1. 按时提交 (满分10分)
        if (submission.getSubmitTime() != null && task.getEndTime() != null
                && !submission.getSubmitTime().isAfter(task.getEndTime())) {
            objective.setOnTimeScore(10);
        } else {
            objective.setOnTimeScore(0);
            objective.addDeduction("未按时提交", 10);
        }

        // 2. 文件格式符合要求 (满分10分)
        if (task.getAllowedFileTypes() != null && !task.getAllowedFileTypes().isEmpty()) {
            String[] allowedTypes = task.getAllowedFileTypes().split(",");
            boolean allMatch = files.stream().allMatch(f -> {
                String ext = "." + f.getFileType().toLowerCase();
                return java.util.Arrays.stream(allowedTypes).anyMatch(t -> t.trim().equalsIgnoreCase(ext));
            });
            if (allMatch && !files.isEmpty()) {
                objective.setFormatScore(10);
            } else {
                objective.setFormatScore(0);
                objective.addDeduction("文件格式不符合要求", 10);
            }
        } else {
            objective.setFormatScore(10);
        }

        // 3. 必要章节检查 (满分30分，每项6分)
        String[] requiredSections = {"需求分析", "系统设计", "数据库设计", "功能实现", "测试", "总结"};
        int sectionScore = 0;
        for (String section : requiredSections) {
            if (content.contains(section)) {
                sectionScore += 6;
            } else {
                objective.addMissingSection(section);
            }
        }
        objective.setSectionScore(sectionScore);

        // 4. 运行截图检查 (满分20分)
        long imageCount = files.stream().filter(f ->
                List.of("JPG", "JPEG", "PNG").contains(f.getFileType())
        ).count();
        long screenshotInText = countPattern(content, "截图|Screenshot|运行结果|界面");
        long totalScreenshots = Math.max(imageCount, screenshotInText);
        int screenshotScore = (int) Math.min(20, totalScreenshots * 5);
        objective.setScreenshotScore(screenshotScore);
        if (totalScreenshots == 0) {
            objective.addDeduction("缺少运行截图", 20);
        }

        // 5. 字数检查 (满分10分)
        int wordCount = content.length();
        if (wordCount >= 2000) {
            objective.setWordScore(10);
        } else if (wordCount >= 1000) {
            objective.setWordScore(5);
        } else {
            objective.setWordScore(0);
            objective.addDeduction("内容字数不足(" + wordCount + "字)", 10);
        }

        // 6. 包含测试结果 (满分20分)
        boolean hasTest = content.contains("测试") || content.contains("Test") || content.contains("用例");
        if (hasTest) {
            objective.setTestScore(20);
        } else {
            objective.setTestScore(0);
            objective.addDeduction("缺少测试相关内容", 20);
        }

        objective.setTotalScore(objective.getOnTimeScore() + objective.getFormatScore()
                + objective.getSectionScore() + objective.getScreenshotScore()
                + objective.getWordScore() + objective.getTestScore());

        return Result.ok(objective.toMap());
    }

    private long countPattern(String text, String pattern) {
        if (text == null || text.isEmpty()) return 0;
        java.util.regex.Matcher matcher = Pattern.compile(pattern).matcher(text);
        long count = 0;
        while (matcher.find()) count++;
        return count;
    }

    /**
     * 成绩修正流程
     */
    @Transactional(rollbackFor = Exception.class)
    public Result<Void> correctScore(Long submissionId, Long indicatorId, BigDecimal newScore, String reason, Long correctedBy) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }

        if (!"PUBLISHED".equals(submission.getScoreStatus())) {
            return Result.error(40901, "只有已发布的成绩才能修正");
        }

        if (reason == null || reason.isEmpty()) {
            return Result.error(40001, "修正原因不能为空");
        }

        ScoreResult scoreResult = null;
        BigDecimal originalScore = submission.getTotalScore();

        if (indicatorId != null) {
            // 修正单项指标
            scoreResult = scoreResultMapper.selectOne(
                    new LambdaQueryWrapper<ScoreResult>()
                            .eq(ScoreResult::getSubmissionId, submissionId)
                            .eq(ScoreResult::getIndicatorId, indicatorId)
            );
            if (scoreResult == null) {
                return Result.error(40401, "评分记录不存在");
            }
            originalScore = scoreResult.getFinalScore();

            // 记录修正
            ScoreCorrection correction = new ScoreCorrection();
            correction.setSubmissionId(submissionId);
            correction.setIndicatorId(indicatorId);
            correction.setOriginalScore(originalScore);
            correction.setNewScore(newScore);
            correction.setReason(reason);
            correction.setCorrectedBy(correctedBy);
            correction.setCorrectedAt(LocalDateTime.now());
            scoreCorrectionMapper.insert(correction);

            // 更新分数
            scoreResult.setFinalScore(newScore);
            scoreResult.setTeacherScore(newScore);
            scoreResultMapper.updateById(scoreResult);

            // 重新计算总分
            recalculateTotalScore(submissionId);
        } else {
            // 修正总分
            ScoreCorrection correction = new ScoreCorrection();
            correction.setSubmissionId(submissionId);
            correction.setOriginalScore(originalScore);
            correction.setNewScore(newScore);
            correction.setReason(reason);
            correction.setCorrectedBy(correctedBy);
            correction.setCorrectedAt(LocalDateTime.now());
            scoreCorrectionMapper.insert(correction);

            submission.setTotalScore(newScore);
            submissionMapper.updateById(submission);
        }

        // 通知学生
        try {
            messageService.sendMessage(
                    submission.getStudentId(),
                    "SCORE_CORRECTED",
                    "成绩修正通知",
                    String.format("您的实训任务（提交ID:%d）成绩已修正，修正原因：%s，请及时查看。", submissionId, reason),
                    submissionId
            );
        } catch (Exception e) {
            log.warn("发送成绩修正消息失败: {}", e.getMessage());
        }

        log.info("成绩修正: submissionId={}, indicatorId={}, original={}, new={}, reason={}",
                submissionId, indicatorId, originalScore, newScore, reason);
        return Result.ok();
    }

    /**
     * 重新计算总分
     */
    private void recalculateTotalScore(Long submissionId) {
        List<ScoreResult> scores = scoreResultMapper.selectList(
                new LambdaQueryWrapper<ScoreResult>().eq(ScoreResult::getSubmissionId, submissionId)
        );

        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) return;

        TrainingTask task = taskMapper.selectById(submission.getTaskId());
        List<Indicator> allIndicators = List.of();
        if (task != null && task.getTemplateId() != null) {
            allIndicators = indicatorMapper.selectList(
                    new LambdaQueryWrapper<Indicator>()
                            .eq(Indicator::getTemplateId, task.getTemplateId())
                            .isNull(Indicator::getParentId)
            );
        }
        final Map<Long, Indicator> indicatorMap = allIndicators.stream()
                .collect(java.util.stream.Collectors.toMap(Indicator::getId, ind -> ind));

        BigDecimal totalScore = BigDecimal.ZERO;
        for (ScoreResult sr : scores) {
            if (sr.getFinalScore() != null) {
                Indicator ind = indicatorMap.get(sr.getIndicatorId());
                if (ind != null && ind.getWeight() != null && ind.getMaxScore() != null && ind.getMaxScore().compareTo(BigDecimal.ZERO) > 0) {
                    BigDecimal contribution = sr.getFinalScore()
                            .divide(ind.getMaxScore(), 4, RoundingMode.HALF_UP)
                            .multiply(ind.getWeight());
                    totalScore = totalScore.add(contribution);
                } else {
                    totalScore = totalScore.add(sr.getFinalScore());
                }
            }
        }

        submission.setTotalScore(totalScore.setScale(2, RoundingMode.HALF_UP));
        submissionMapper.updateById(submission);
    }

    /**
     * 客观评分结果
     */
    @lombok.Data
    public static class ObjectiveScoreResult {
        private int onTimeScore = 0;
        private int formatScore = 0;
        private int sectionScore = 0;
        private int screenshotScore = 0;
        private int wordScore = 0;
        private int testScore = 0;
        private int totalScore = 0;
        private List<String> missingSections = new java.util.ArrayList<>();
        private List<String> deductions = new java.util.ArrayList<>();

        public void addMissingSection(String section) {
            missingSections.add(section);
        }

        public void addDeduction(String reason, int points) {
            deductions.add(reason + " (-" + points + "分)");
        }

        public Map<String, Object> toMap() {
            Map<String, Object> map = new java.util.LinkedHashMap<>();
            map.put("onTimeScore", onTimeScore);
            map.put("formatScore", formatScore);
            map.put("sectionScore", sectionScore);
            map.put("screenshotScore", screenshotScore);
            map.put("wordScore", wordScore);
            map.put("testScore", testScore);
            map.put("totalScore", totalScore);
            map.put("missingSections", missingSections);
            map.put("deductions", deductions);
            return map;
        }
    }
}
