package com.bisai.controller;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.TrainingTask;
import com.bisai.service.TaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public Result<PageResult<TrainingTask>> list(PageQuery query,
                                                  @RequestParam(required = false) Long courseId,
                                                  @RequestParam(required = false) String status) {
        return taskService.listTasks(query, courseId, status);
    }

    @GetMapping("/{id}")
    public Result<TrainingTask> get(@PathVariable Long id) {
        return taskService.getTask(id);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<TrainingTask> create(@RequestBody TrainingTask task) {
        return taskService.createTask(task);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<TrainingTask> update(@PathVariable Long id, @RequestBody TrainingTask task) {
        return taskService.updateTask(id, task);
    }

    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> publish(@PathVariable Long id) {
        return taskService.publishTask(id);
    }

    @PutMapping("/{id}/close")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> close(@PathVariable Long id) {
        return taskService.closeTask(id);
    }

    // 批量解析 - 对任务下所有提交触发解析
    @PostMapping("/{id}/batch-parse")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> batchParse(@PathVariable Long id) {
        return taskService.batchParse(id);
    }

    // 批量评分 - 对任务下所有提交触发评分
    @PostMapping("/{id}/batch-score")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> batchScore(@PathVariable Long id) {
        return taskService.batchScore(id);
    }

    // 批量核查 - 对任务下所有提交触发核查
    @PostMapping("/{id}/batch-check")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> batchCheck(@PathVariable Long id) {
        return taskService.batchCheck(id);
    }

    // 批量操作进度查询
    @GetMapping("/{id}/batch-progress")
    public Result<Map<String, Object>> batchProgress(@PathVariable Long id) {
        return taskService.getBatchProgress(id);
    }
}
