package com.bisai.controller;

import com.bisai.common.Result;
import com.bisai.entity.AsyncTask;
import com.bisai.service.AsyncTaskService;
import com.bisai.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/async-tasks")
@RequiredArgsConstructor
public class AsyncTaskController {

    private final AsyncTaskService asyncTaskService;
    private final PermissionService permissionService;

    @GetMapping("/{taskId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<AsyncTask> getTaskStatus(@PathVariable Long taskId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        AsyncTask task = asyncTaskService.getTaskStatus(taskId);
        if (!permissionService.isAdmin(role)) {
            if (task == null) {
                return Result.error(40401, "任务不存在");
            }
            if (!permissionService.isTeacherOwnerOfSubmission(task.getBizId(), userId)) {
                return Result.error(40301, "无权访问该任务");
            }
        }
        return Result.ok(task);
    }

    @GetMapping("/biz/{bizId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public Result<List<AsyncTask>> getTasksByBizId(@PathVariable Long bizId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        if (!permissionService.isAdmin(role)
                && !permissionService.isTeacherOwnerOfSubmission(bizId, userId)
                && !permissionService.isStudentOwnerOfSubmission(bizId, userId)) {
            return Result.error(40301, "无权访问该任务");
        }
        List<AsyncTask> tasks = asyncTaskService.getTasksByBizId(bizId);
        // 学生角色过滤敏感字段，避免泄漏 errorMessage/retryCount 等内部状态
        if ("STUDENT".equals(role)) {
            tasks.forEach(t -> {
                t.setErrorMessage(null);
                t.setRetryCount(null);
                t.setMaxRetry(null);
                t.setNextRunAt(null);
            });
        }
        return Result.ok(tasks);
    }

    @PostMapping("/{taskId}/retry")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> retryTask(@PathVariable Long taskId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        if (!permissionService.isAdmin(role)) {
            AsyncTask task = asyncTaskService.getTaskStatus(taskId);
            if (task == null) {
                return Result.error(40401, "任务不存在");
            }
            if (!permissionService.isTeacherOwnerOfSubmission(task.getBizId(), userId)) {
                return Result.error(40301, "无权操作该任务");
            }
        }
        boolean success = asyncTaskService.retryFailedTask(taskId);
        return success ? Result.ok() : Result.error("任务不存在或状态不是失败");
    }

    @PostMapping("/{taskId}/cancel")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> cancelTask(@PathVariable Long taskId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        if (!permissionService.isAdmin(role)) {
            AsyncTask task = asyncTaskService.getTaskStatus(taskId);
            if (task == null) {
                return Result.error(40401, "任务不存在");
            }
            if (!permissionService.isTeacherOwnerOfSubmission(task.getBizId(), userId)) {
                return Result.error(40301, "无权操作该任务");
            }
        }
        boolean success = asyncTaskService.cancelTask(taskId);
        return success ? Result.ok() : Result.error("任务不存在或无法取消");
    }

    @PostMapping("/{taskId}/force-reset")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> forceResetTask(@PathVariable Long taskId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        if (!permissionService.isAdmin(role)) {
            AsyncTask task = asyncTaskService.getTaskStatus(taskId);
            if (task == null) {
                return Result.error(40401, "任务不存在");
            }
            if (!permissionService.isTeacherOwnerOfSubmission(task.getBizId(), userId)) {
                return Result.error(40301, "无权操作该任务");
            }
        }
        boolean success = asyncTaskService.forceResetTask(taskId);
        return success ? Result.ok() : Result.error("任务不存在或当前状态无法重置");
    }

    @PostMapping("/batch-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Map<String, Long>> getBatchStatus(@RequestBody List<Long> taskIds, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("");
        // 一次 IN 查询替代 stream 内逐个查询（N+1）
        List<AsyncTask> tasks = asyncTaskService.getTasksByIds(taskIds);
        if (!permissionService.isAdmin(role)) {
            tasks = tasks.stream()
                    .filter(t -> permissionService.isTeacherOwnerOfSubmission(t.getBizId(), userId))
                    .toList();
        }
        return Result.ok(asyncTaskService.getBatchStatus(tasks));
    }
}
