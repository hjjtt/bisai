package com.bisai.controller;

import com.bisai.common.Result;
import com.bisai.entity.AsyncTask;
import com.bisai.service.AsyncTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/async-tasks")
@RequiredArgsConstructor
public class AsyncTaskController {

    private final AsyncTaskService asyncTaskService;

    @GetMapping("/{taskId}")
    public Result<AsyncTask> getTaskStatus(@PathVariable Long taskId) {
        return Result.ok(asyncTaskService.getTaskStatus(taskId));
    }

    @GetMapping("/biz/{bizId}")
    public Result<List<AsyncTask>> getTasksByBizId(@PathVariable Long bizId) {
        return Result.ok(asyncTaskService.getTasksByBizId(bizId));
    }

    @PostMapping("/{taskId}/retry")
    public Result<Void> retryTask(@PathVariable Long taskId) {
        boolean success = asyncTaskService.retryFailedTask(taskId);
        return success ? Result.ok() : Result.error("任务不存在或状态不是失败");
    }

    @PostMapping("/batch-status")
    public Result<Map<String, Long>> getBatchStatus(@RequestBody List<Long> taskIds) {
        Map<String, Long> stats = Map.of(
                "total", (long) taskIds.size(),
                "pending", taskIds.stream().filter(id -> {
                    AsyncTask t = asyncTaskService.getTaskStatus(id);
                    return t != null && "PENDING".equals(t.getStatus());
                }).count(),
                "running", taskIds.stream().filter(id -> {
                    AsyncTask t = asyncTaskService.getTaskStatus(id);
                    return t != null && "RUNNING".equals(t.getStatus());
                }).count(),
                "success", taskIds.stream().filter(id -> {
                    AsyncTask t = asyncTaskService.getTaskStatus(id);
                    return t != null && "SUCCESS".equals(t.getStatus());
                }).count(),
                "failed", taskIds.stream().filter(id -> {
                    AsyncTask t = asyncTaskService.getTaskStatus(id);
                    return t != null && "FAILED".equals(t.getStatus());
                }).count()
        );
        return Result.ok(stats);
    }
}
