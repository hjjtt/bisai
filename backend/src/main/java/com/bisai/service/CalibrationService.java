package com.bisai.service;

import com.bisai.common.Result;
import com.bisai.entity.ScoreCalibration;
import com.bisai.entity.Indicator;
import com.bisai.entity.Submission;
import com.bisai.entity.TrainingTask;
import com.bisai.mapper.ScoreCalibrationMapper;
import com.bisai.mapper.IndicatorMapper;
import com.bisai.mapper.SubmissionMapper;
import com.bisai.mapper.TrainingTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CalibrationService {

    private final ScoreCalibrationMapper calibrationMapper;
    private final IndicatorMapper indicatorMapper;
    private final SubmissionMapper submissionMapper;
    private final TrainingTaskMapper taskMapper;

    /**
     * 保存校准样本
     */
    public Result<Void> saveCalibration(Long taskId, Long submissionId, List<Map<String, Object>> items, Long confirmedBy) {
        TrainingTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return Result.error(40401, "任务不存在");
        }

        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }

        // 删除该校准样本的旧记录
        calibrationMapper.delete(
                new LambdaQueryWrapper<ScoreCalibration>()
                        .eq(ScoreCalibration::getTaskId, taskId)
                        .eq(ScoreCalibration::getSubmissionId, submissionId)
        );

        // 保存新的校准记录
        for (Map<String, Object> item : items) {
            ScoreCalibration calibration = new ScoreCalibration();
            calibration.setTaskId(taskId);
            calibration.setSubmissionId(submissionId);
            calibration.setIndicatorId(Long.valueOf(item.get("indicatorId").toString()));
            calibration.setCalibrationScore(new java.math.BigDecimal(item.get("score").toString()));
            calibration.setCalibrationReason((String) item.get("reason"));
            calibration.setTypicalAdvantages((String) item.get("advantages"));
            calibration.setTypicalProblems((String) item.get("problems"));
            calibration.setDeductionBasis((String) item.get("deductionBasis"));
            calibration.setConfirmedBy(confirmedBy);
            calibration.setConfirmedAt(java.time.LocalDateTime.now());
            calibrationMapper.insert(calibration);
        }

        log.info("评分校准样本已保存: taskId={}, submissionId={}", taskId, submissionId);
        return Result.ok();
    }

    /**
     * 获取任务的校准样本
     */
    public Result<List<ScoreCalibration>> getCalibrations(Long taskId) {
        List<ScoreCalibration> calibrations = calibrationMapper.selectList(
                new LambdaQueryWrapper<ScoreCalibration>()
                        .eq(ScoreCalibration::getTaskId, taskId)
                        .orderByAsc(ScoreCalibration::getSubmissionId)
        );

        // 填充指标名称
        if (!calibrations.isEmpty()) {
            List<Indicator> indicators = indicatorMapper.selectList(
                    new LambdaQueryWrapper<Indicator>()
                            .in(Indicator::getId, calibrations.stream()
                                    .map(ScoreCalibration::getIndicatorId)
                                    .collect(Collectors.toSet()))
            );
            Map<Long, String> nameMap = indicators.stream()
                    .collect(Collectors.toMap(Indicator::getId, Indicator::getName));
            calibrations.forEach(c -> {
                if (nameMap.containsKey(c.getIndicatorId())) {
                    c.setIndicatorName(nameMap.get(c.getIndicatorId()));
                }
            });
        }

        return Result.ok(calibrations);
    }

    /**
     * 获取校准参考文本（用于AI评分Prompt）
     */
    public String getCalibrationContext(Long taskId) {
        List<ScoreCalibration> calibrations = calibrationMapper.selectList(
                new LambdaQueryWrapper<ScoreCalibration>()
                        .eq(ScoreCalibration::getTaskId, taskId)
        );

        if (calibrations.isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder("## 评分校准参考\n");
        context.append("以下是教师确认的校准样本，请参照相同的评分尺度进行评分：\n\n");

        // 按提交分组
        Map<Long, List<ScoreCalibration>> grouped = calibrations.stream()
                .collect(Collectors.groupingBy(ScoreCalibration::getSubmissionId));

        int sampleNum = 1;
        for (Map.Entry<Long, List<ScoreCalibration>> entry : grouped.entrySet()) {
            context.append("### 校准样本").append(sampleNum++).append(" (提交ID: ").append(entry.getKey()).append(")\n");
            for (ScoreCalibration c : entry.getValue()) {
                context.append("- 指标: ").append(c.getIndicatorName() != null ? c.getIndicatorName() : c.getIndicatorId())
                        .append(", 分数: ").append(c.getCalibrationScore())
                        .append(", 理由: ").append(c.getCalibrationReason() != null ? c.getCalibrationReason() : "")
                        .append("\n");
                if (c.getTypicalAdvantages() != null) {
                    context.append("  优点: ").append(c.getTypicalAdvantages()).append("\n");
                }
                if (c.getTypicalProblems() != null) {
                    context.append("  问题: ").append(c.getTypicalProblems()).append("\n");
                }
            }
            context.append("\n");
        }

        return context.toString();
    }
}
