package com.bisai.controller;

import com.bisai.common.Result;
import com.bisai.entity.ScorePairwise;
import com.bisai.service.ScoreConsistencyService;
import com.bisai.service.ScorePairwiseService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * P2.9: 评分一致性看板 + P2.8: Pairwise 比较 API
 */
@RestController
@RequestMapping("/api/consistency")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class ScoreConsistencyController {

    private final ScoreConsistencyService consistencyService;
    private final ScorePairwiseService pairwiseService;

    // ==================== P0.3 + P2.9: 校准相关性 + 一致性看板 ====================

    /**
     * 获取指定任务的 AI vs 教师评分相关性
     */
    @GetMapping("/correlation")
    public Result<Map<String, Object>> getCorrelation(@RequestParam(required = false) Long taskId) {
        return Result.ok(consistencyService.computeTaskCorrelation(taskId));
    }

    /**
     * 获取全局一致性趋势（管理员看板用）
     */
    @GetMapping("/trend")
    public Result<Map<String, Object>> getConsistencyTrend(
            @RequestParam(defaultValue = "30") int days) {
        return Result.ok(consistencyService.getConsistencyTrend(days));
    }

    /**
     * 获取各任务的一致性汇总
     */
    @GetMapping("/tasks-summary")
    public Result<List<Map<String, Object>>> getTaskConsistencySummary() {
        return Result.ok(consistencyService.getTaskConsistencySummary());
    }

    /**
     * 手动触发生成一致性快照
     */
    @PostMapping("/snapshot")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> generateSnapshot() {
        consistencyService.generateSnapshot();
        return Result.ok();
    }

    // ==================== P2.8: Pairwise 比较 ====================

    /**
     * 对任务下所有提交执行 Pairwise 比较
     */
    @PostMapping("/pairwise/task/{taskId}")
    public Result<Integer> compareTask(@PathVariable Long taskId) {
        return pairwiseService.compareAllForTask(taskId);
    }

    /**
     * 获取任务的 Pairwise 比较结果
     */
    @GetMapping("/pairwise/task/{taskId}")
    public Result<List<ScorePairwise>> getPairwiseResults(@PathVariable Long taskId) {
        return pairwiseService.getComparisons(taskId);
    }

    /**
     * 获取基于 Pairwise 的排名
     */
    @GetMapping("/pairwise/ranking/{taskId}")
    public Result<List<Map<String, Object>>> getRanking(@PathVariable Long taskId) {
        return pairwiseService.getRanking(taskId);
    }
}
