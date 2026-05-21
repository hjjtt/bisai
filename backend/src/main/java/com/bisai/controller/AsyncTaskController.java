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
        if (!permissionService.isAdmin(role)) {
            AsyncTask task = asyncTaskService.getTaskStatus(taskId);
            if (task == null || !permissionService.isTeacherOwnerOfSubmission(task.getBizId(), userId)) {
                return Result.error(40301, "无权访问该任务");
            }
        }
        return Result.ok(asyncTaskService.getTaskStatus(taskId));
    }

    @GetMapping("/biz/{bizId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<AsyncTask>> getTasksByBizId(@PathVariable Long bizId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        if (!permissionService.isAdmin(role) && !permissionService.isTeacherOwnerOfSubmission(bizId, userId)) {
            return Result.error(40301, "无权访问该任务");
        }
        return Result.ok(asyncTaskService.getTasksByBizId(bizId));
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
            if (task == null || !permissionService.isTeacherOwnerOfSubmission(task.getBizId(), userId)) {
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
            if (task == null || !permissionService.isTeacherOwnerOfSubmission(task.getBizId(), userId)) {
                return Result.error(40301, "无权操作该任务");
            }
        }
        boolean success = asyncTaskService.cancelTask(taskId);
        return success ? Result.ok() : Result.error("任务不存在或无法取消");
    }

    @PostMapping("/batch-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Map<String, Long>> getBatchStatus(@RequestBody List<Long> taskIds, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("");
        if (!permissionService.isAdmin(role)) {
            taskIds = taskIds.stream().filter(tid -> {
                AsyncTask task = asyncTaskService.getTaskStatus(tid);
                return task != null && permissionService.isTeacherOwnerOfSubmission(task.getBizId(), userId);
            }).toList();
        }
        return Result.ok(asyncTaskService.getBatchStatus(taskIds));
    }
}
