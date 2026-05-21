package com.bisai.controller;

import com.bisai.common.Result;
import com.bisai.entity.AsyncTask;
import com.bisai.service.AsyncTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/async-tasks")
@RequiredArgsConstructor
public class AsyncTaskController {

    private final AsyncTaskService asyncTaskService;

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<AsyncTask> getTaskStatus(@PathVariable Long taskId) {
        return Result.ok(asyncTaskService.getTaskStatus(taskId));
    }

    @GetMapping("/biz/{bizId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<AsyncTask>> getTasksByBizId(@PathVariable Long bizId) {
        return Result.ok(asyncTaskService.getTasksByBizId(bizId));
    }

    @PostMapping("/{taskId}/retry")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> retryTask(@PathVariable Long taskId) {
        boolean success = asyncTaskService.retryFailedTask(taskId);
        return success ? Result.ok() : Result.error("任务不存在或状态不是失败");
    }

    @PostMapping("/{taskId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> cancelTask(@PathVariable Long taskId) {
        boolean success = asyncTaskService.cancelTask(taskId);
        return success ? Result.ok() : Result.error("任务不存在或无法取消");
    }

    @PostMapping("/batch-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Map<String, Long>> getBatchStatus(@RequestBody List<Long> taskIds) {
        return Result.ok(asyncTaskService.getBatchStatus(taskIds));
    }
}
