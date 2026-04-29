package com.bisai.service;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.Submission;
import com.bisai.entity.TrainingTask;
import com.bisai.mapper.SubmissionMapper;
import com.bisai.mapper.TrainingTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TrainingTaskMapper taskMapper;
    private final SubmissionMapper submissionMapper;
    private final AiService aiService;

    public Result<PageResult<TrainingTask>> listTasks(PageQuery query, Long courseId, String status) {
        Page<TrainingTask> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<TrainingTask> wrapper = new LambdaQueryWrapper<>();

        if (courseId != null) {
            wrapper.eq(TrainingTask::getCourseId, courseId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(TrainingTask::getStatus, status);
        }
        wrapper.orderByDesc(TrainingTask::getCreatedAt);

        Page<TrainingTask> result = taskMapper.selectPage(page, wrapper);
        return Result.ok(new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    public Result<TrainingTask> getTask(Long id) {
        TrainingTask task = taskMapper.selectById(id);
        if (task == null) {
            return Result.error(40401, "任务不存在");
        }
        return Result.ok(task);
    }

    public Result<TrainingTask> createTask(TrainingTask task) {
        task.setStatus("DRAFT");
        taskMapper.insert(task);
        return Result.ok(task);
    }

    public Result<TrainingTask> updateTask(Long id, TrainingTask task) {
        task.setId(id);
        taskMapper.updateById(task);
        return Result.ok(taskMapper.selectById(id));
    }

    public Result<Void> publishTask(Long id) {
        TrainingTask task = taskMapper.selectById(id);
        if (task == null) {
            return Result.error(40401, "任务不存在");
        }
        if (!"DRAFT".equals(task.getStatus())) {
            return Result.error(40902, "只有草稿状态的任务可以发布");
        }
        task.setStatus("PUBLISHED");
        taskMapper.updateById(task);
        return Result.ok();
    }

    public Result<Void> closeTask(Long id) {
        TrainingTask task = taskMapper.selectById(id);
        if (task == null) {
            return Result.error(40401, "任务不存在");
        }
        task.setStatus("CLOSED");
        taskMapper.updateById(task);
        return Result.ok();
    }

    /**
     * 批量解析 - 调用 AI 逐个解析任务下所有提交
     */
    public Result<Void> batchParse(Long taskId) {
        TrainingTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return Result.error(40401, "任务不存在");
        }
        java.util.List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>().eq(Submission::getTaskId, taskId)
        );
        for (Submission sub : submissions) {
            try {
                aiService.doParse(sub.getId());
            } catch (Exception e) {
                log.warn("批量解析-提交{}失败: {}", sub.getId(), e.getMessage());
            }
        }
        return Result.ok();
    }

    /**
     * 批量评分 - 调用 AI 逐个评分任务下所有提交
     */
    public Result<Void> batchScore(Long taskId) {
        TrainingTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return Result.error(40401, "任务不存在");
        }
        java.util.List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>().eq(Submission::getTaskId, taskId)
        );
        for (Submission sub : submissions) {
            try {
                aiService.doScore(sub.getId());
            } catch (Exception e) {
                log.warn("批量评分-提交{}失败: {}", sub.getId(), e.getMessage());
            }
        }
        return Result.ok();
    }

    /**
     * 查询批量操作进度
     */
    public Result<Map<String, Object>> getBatchProgress(Long taskId) {
        Long total = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>().eq(Submission::getTaskId, taskId)
        );
        Long success = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getTaskId, taskId)
                        .ne(Submission::getScoreStatus, "NOT_SCORED")
        );
        Long failed = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getTaskId, taskId)
                        .eq(Submission::getParseStatus, "FAILED")
        );
        Map<String, Object> progress = new HashMap<>();
        progress.put("total", total);
        progress.put("success", success);
        progress.put("failed", failed);
        progress.put("running", Math.max(0, total - success - failed));
        return Result.ok(progress);
    }
}
