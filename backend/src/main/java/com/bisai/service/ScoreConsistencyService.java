package com.bisai.service;

import com.bisai.common.Result;
import com.bisai.entity.*;
import com.bisai.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 评分一致性分析服务
 *
 * P0.3: 校准相关系数（AI vs 教师）
 * P2.9: 一致性看板数据
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreConsistencyService {

    private final ScoreResultMapper scoreResultMapper;
    private final SubmissionMapper submissionMapper;
    private final TrainingTaskMapper taskMapper;
    private final ScoreConsistencySnapshotMapper snapshotMapper;
    private final ScoreJudgeService scoreJudgeService;

    /**
     * 计算指定任务的 AI vs 教师评分相关性
     *
     * @param taskId 任务 ID，null 表示全局
     * @return 统计指标
     */
    public Map<String, Object> computeTaskCorrelation(Long taskId) {
        // 查找教师已确认/已发布的评分结果
        List<Submission> submissions;
        if (taskId != null) {
            submissions = submissionMapper.selectList(
                    new LambdaQueryWrapper<Submission>()
                            .eq(Submission::getTaskId, taskId)
                            .in(Submission::getScoreStatus, "TEACHER_CONFIRMED", "PUBLISHED")
            );
        } else {
            submissions = submissionMapper.selectList(
                    new LambdaQueryWrapper<Submission>()
                            .in(Submission::getScoreStatus, "TEACHER_CONFIRMED", "PUBLISHED")
            );
        }

        if (submissions.isEmpty()) {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("sampleSize", 0);
            empty.put("warning", "暂无教师确认的评分数据");
            return empty;
        }

        // 收集 AI 分数和教师分数
        List<Long> submissionIds = submissions.stream().map(Submission::getId).collect(Collectors.toList());
        List<ScoreResult> results = scoreResultMapper.selectList(
                new LambdaQueryWrapper<ScoreResult>()
                        .in(ScoreResult::getSubmissionId, submissionIds)
        );

        // 按 submission 分组，取总分级别的对比
        Map<Long, BigDecimal> aiTotalMap = new LinkedHashMap<>();
        Map<Long, BigDecimal> teacherTotalMap = new LinkedHashMap<>();

        for (Submission sub : submissions) {
            if (sub.getAutoTotalScore() != null && sub.getTotalScore() != null) {
                aiTotalMap.put(sub.getId(), sub.getAutoTotalScore());
                teacherTotalMap.put(sub.getId(), sub.getTotalScore());
            }
        }

        if (aiTotalMap.size() < 3) {
            Map<String, Object> small = new LinkedHashMap<>();
            small.put("sampleSize", aiTotalMap.size());
            small.put("warning", "教师确认数据不足（<3条），统计结果不可靠");
            // 仍然计算
            if (!aiTotalMap.isEmpty()) {
                List<BigDecimal> aiScores = new ArrayList<>(aiTotalMap.values());
                List<BigDecimal> teacherScores = new ArrayList<>(teacherTotalMap.values());
                small.putAll(scoreJudgeService.computeCalibrationStats(aiScores, teacherScores));
            }
            return small;
        }

        List<BigDecimal> aiScores = new ArrayList<>(aiTotalMap.values());
        List<BigDecimal> teacherScores = new ArrayList<>(teacherTotalMap.values());

        Map<String, Object> stats = scoreJudgeService.computeCalibrationStats(aiScores, teacherScores);

        // 指标级别的分析
        stats.put("indicatorAnalysis", computeIndicatorLevelAnalysis(results, submissionIds));

        // 交叉模型一致率
        long crossModelTotal = results.stream()
                .filter(r -> r.getCrossModelScore() != null).count();
        long crossModelAgree = results.stream()
                .filter(r -> r.getCrossModelDivergence() != null && r.getCrossModelDivergence().doubleValue() <= 15.0)
                .count();
        if (crossModelTotal > 0) {
            double agreement = (double) crossModelAgree / crossModelTotal;
            stats.put("crossModelAgreement", Math.round(agreement * 10000.0) / 10000.0);
            stats.put("crossModelTotal", crossModelTotal);
        }

        return stats;
    }

    /**
     * 按指标级别的 AI vs 教师对比
     */
    private List<Map<String, Object>> computeIndicatorLevelAnalysis(List<ScoreResult> results, List<Long> submissionIds) {
        // 只取有教师评分的
        List<ScoreResult> withTeacher = results.stream()
                .filter(r -> r.getTeacherScore() != null && r.getAutoScore() != null)
                .collect(Collectors.toList());

        if (withTeacher.isEmpty()) return List.of();

        // 按指标分组
        Map<Long, List<ScoreResult>> grouped = withTeacher.stream()
                .collect(Collectors.groupingBy(ScoreResult::getIndicatorId));

        List<Map<String, Object>> analysis = new ArrayList<>();
        for (Map.Entry<Long, List<ScoreResult>> entry : grouped.entrySet()) {
            Long indicatorId = entry.getKey();
            List<ScoreResult> items = entry.getValue();

            List<BigDecimal> aiScores = items.stream().map(ScoreResult::getAutoScore).collect(Collectors.toList());
            List<BigDecimal> teacherScores = items.stream().map(ScoreResult::getTeacherScore).collect(Collectors.toList());

            Map<String, Object> indicatorStats = scoreJudgeService.computeCalibrationStats(aiScores, teacherScores);
            indicatorStats.put("indicatorId", indicatorId);
            if (!items.isEmpty()) {
                indicatorStats.put("indicatorName", items.get(0).getIndicatorName());
            }
            analysis.add(indicatorStats);
        }
        return analysis;
    }

    /**
     * 生成一致性统计快照（可定时调用）
     */
    public void generateSnapshot() {
        LocalDate today = LocalDate.now();

        // 全局快照
        Map<String, Object> globalStats = computeTaskCorrelation(null);
        saveSnapshot(null, today, globalStats);

        // 按任务快照
        List<TrainingTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<TrainingTask>().eq(TrainingTask::getStatus, "PUBLISHED")
        );
        for (TrainingTask task : tasks) {
            Map<String, Object> taskStats = computeTaskCorrelation(task.getId());
            saveSnapshot(task.getId(), today, taskStats);
        }

        log.info("评分一致性快照已生成, date={}", today);
    }

    private void saveSnapshot(Long taskId, LocalDate date, Map<String, Object> stats) {
        // 删除同一天同任务的旧快照
        if (taskId == null) {
            snapshotMapper.delete(
                    new LambdaQueryWrapper<ScoreConsistencySnapshot>()
                            .isNull(ScoreConsistencySnapshot::getTaskId)
                            .eq(ScoreConsistencySnapshot::getSnapshotDate, date)
            );
        } else {
            snapshotMapper.delete(
                    new LambdaQueryWrapper<ScoreConsistencySnapshot>()
                            .eq(ScoreConsistencySnapshot::getTaskId, taskId)
                            .eq(ScoreConsistencySnapshot::getSnapshotDate, date)
            );
        }

        ScoreConsistencySnapshot snapshot = new ScoreConsistencySnapshot();
        snapshot.setTaskId(taskId);
        snapshot.setSnapshotDate(date);
        snapshot.setTotalEvaluated(toInt(stats.get("sampleSize")));
        snapshot.setTotalTeacherConfirmed(toInt(stats.get("sampleSize")));

        Object pearson = stats.get("pearsonCorrelation");
        if (pearson != null) snapshot.setPearsonCorrelation(toBigDecimal(pearson));

        Object spearman = stats.get("spearmanCorrelation");
        if (spearman != null) snapshot.setSpearmanCorrelation(toBigDecimal(spearman));

        Object rmse = stats.get("rmse");
        if (rmse != null) snapshot.setRmse(toBigDecimal(rmse));

        Object mae = stats.get("mae");
        if (mae != null) snapshot.setMae(toBigDecimal(mae));

        Object avgDiv = stats.get("avgDivergence");
        if (avgDiv != null) snapshot.setAvgDivergence(toBigDecimal(avgDiv));

        Object agreement = stats.get("crossModelAgreement");
        if (agreement != null) snapshot.setCrossModelAgreement(toBigDecimal(agreement));

        snapshotMapper.insert(snapshot);
    }

    /**
     * 获取一致性趋势数据（管理员看板用）
     */
    public Map<String, Object> getConsistencyTrend(int days) {
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(days - 1);

        List<ScoreConsistencySnapshot> snapshots = snapshotMapper.selectList(
                new LambdaQueryWrapper<ScoreConsistencySnapshot>()
                        .isNull(ScoreConsistencySnapshot::getTaskId) // 全局快照
                        .ge(ScoreConsistencySnapshot::getSnapshotDate, startDate)
                        .orderByAsc(ScoreConsistencySnapshot::getSnapshotDate)
        );

        // 如果没有快照数据，实时计算一次
        if (snapshots.isEmpty()) {
            generateSnapshot();
            snapshots = snapshotMapper.selectList(
                    new LambdaQueryWrapper<ScoreConsistencySnapshot>()
                            .isNull(ScoreConsistencySnapshot::getTaskId)
                            .ge(ScoreConsistencySnapshot::getSnapshotDate, startDate)
                            .orderByAsc(ScoreConsistencySnapshot::getSnapshotDate)
            );
        }

        List<String> dates = new ArrayList<>();
        List<Double> pearsonValues = new ArrayList<>();
        List<Double> maeValues = new ArrayList<>();
        List<Double> divergenceValues = new ArrayList<>();

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("MM-dd");
        for (ScoreConsistencySnapshot s : snapshots) {
            dates.add(s.getSnapshotDate().format(fmt));
            pearsonValues.add(s.getPearsonCorrelation() != null ? s.getPearsonCorrelation().doubleValue() : 0);
            maeValues.add(s.getMae() != null ? s.getMae().doubleValue() : 0);
            divergenceValues.add(s.getAvgDivergence() != null ? s.getAvgDivergence().doubleValue() : 0);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("dates", dates);
        result.put("pearsonCorrelation", pearsonValues);
        result.put("mae", maeValues);
        result.put("avgDivergence", divergenceValues);

        // 当前最新的统计摘要
        if (!snapshots.isEmpty()) {
            ScoreConsistencySnapshot latest = snapshots.get(snapshots.size() - 1);
            result.put("latestPearson", latest.getPearsonCorrelation());
            result.put("latestSpearman", latest.getSpearmanCorrelation());
            result.put("latestMae", latest.getMae());
            result.put("latestRmse", latest.getRmse());
            result.put("totalEvaluated", latest.getTotalEvaluated());
        }

        return result;
    }

    /**
     * 获取各任务的一致性汇总（管理员看板用）
     */
    public List<Map<String, Object>> getTaskConsistencySummary() {
        List<TrainingTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<TrainingTask>().eq(TrainingTask::getStatus, "PUBLISHED")
        );

        List<Map<String, Object>> summary = new ArrayList<>();
        for (TrainingTask task : tasks) {
            // 取该任务最近的快照
            ScoreConsistencySnapshot snapshot = snapshotMapper.selectOne(
                    new LambdaQueryWrapper<ScoreConsistencySnapshot>()
                            .eq(ScoreConsistencySnapshot::getTaskId, task.getId())
                            .orderByDesc(ScoreConsistencySnapshot::getSnapshotDate)
                            .last("LIMIT 1")
            );

            if (snapshot == null) continue;

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("taskId", task.getId());
            item.put("taskTitle", task.getTitle());
            item.put("pearson", snapshot.getPearsonCorrelation());
            item.put("mae", snapshot.getMae());
            item.put("totalEvaluated", snapshot.getTotalEvaluated());
            summary.add(item);
        }

        // 按 Pearson 相关系数升序排列（最差的排在前面，需要关注）
        summary.sort((a, b) -> {
            Double pa = a.get("pearson") != null ? ((BigDecimal) a.get("pearson")).doubleValue() : 1.0;
            Double pb = b.get("pearson") != null ? ((BigDecimal) b.get("pearson")).doubleValue() : 1.0;
            return pa.compareTo(pb);
        });

        return summary;
    }

    private int toInt(Object val) {
        if (val == null) return 0;
        return Integer.parseInt(val.toString());
    }

    private BigDecimal toBigDecimal(Object val) {
        if (val == null) return null;
        return new BigDecimal(val.toString());
    }
}
