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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Semaphore;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TrainingTaskMapper taskMapper;
    private final SubmissionMapper submissionMapper;
    private final AsyncTaskService asyncTaskService;

    // 批量任务并发控制
    private static final int MAX_CONCURRENT_BATCHES = 3;
    private static final Semaphore batchSemaphore = new Semaphore(MAX_CONCURRENT_BATCHES);
    private static final Map<Long, BatchJob> activeJobs = new ConcurrentHashMap<>();

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
     * 批量解析 - 使用异步任务队列，控制并发
     */
    public Result<Map<String, Object>> batchParse(Long taskId) {
        if (!batchSemaphore.tryAcquire()) {
            return Result.error(42901, "批量任务并发数已达上限(" + MAX_CONCURRENT_BATCHES + ")，请稍后重试");
        }

        TrainingTask task = taskMapper.selectById(taskId);
        if (task == null) {
            batchSemaphore.release();
            return Result.error(40401, "任务不存在");
        }

        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>().eq(Submission::getTaskId, taskId)
        );

        int created = 0;
        for (Submission sub : submissions) {
            asyncTaskService.createTask("PARSE", sub.getId());
            created++;
        }

        activeJobs.put(taskId, new BatchJob(taskId, "PARSE", submissions.size()));

        Map<String, Object> result = new HashMap<>();
        result.put("total", submissions.size());
        result.put("created", created);
        return Result.ok(result);
    }

    /**
     * 批量评分 - 使用异步任务队列，控制并发
     */
    public Result<Map<String, Object>> batchScore(Long taskId) {
        if (!batchSemaphore.tryAcquire()) {
            return Result.error(42901, "批量任务并发数已达上限(" + MAX_CONCURRENT_BATCHES + ")，请稍后重试");
        }

        TrainingTask task = taskMapper.selectById(taskId);
        if (task == null) {
            batchSemaphore.release();
            return Result.error(40401, "任务不存在");
        }

        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>().eq(Submission::getTaskId, taskId)
        );

        int created = 0;
        for (Submission sub : submissions) {
            asyncTaskService.createTask("SCORE", sub.getId());
            created++;
        }

        activeJobs.put(taskId, new BatchJob(taskId, "SCORE", submissions.size()));

        Map<String, Object> result = new HashMap<>();
        result.put("total", submissions.size());
        result.put("created", created);
        return Result.ok(result);
    }

    /**
     * 批量任务完成后释放信号量（由 AsyncTaskService 回调）
     */
    public void onBatchJobCompleted(Long taskId) {
        activeJobs.remove(taskId);
        batchSemaphore.release();
        log.info("批量任务完成，释放信号量: taskId={}", taskId);
    }

    /**
     * 查询批量操作进度
     */
    public Result<Map<String, Object>> getBatchProgress(Long taskId) {
        Long total = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>().eq(Submission::getTaskId, taskId)
        );
        Long parsed = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getTaskId, taskId)
                        .eq(Submission::getParseStatus, "SUCCESS")
        );
        Long scored = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getTaskId, taskId)
                        .eq(Submission::getScoreStatus, "AI_SCORED")
        );
        Long failed = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getTaskId, taskId)
                        .in(Submission::getParseStatus, "FAILED")
        );

        Map<String, Object> progress = new HashMap<>();
        progress.put("total", total);
        progress.put("parsed", parsed);
        progress.put("scored", scored);
        progress.put("failed", failed);
        progress.put("running", Math.max(0, total - parsed - failed));
        return Result.ok(progress);
    }

    /**
     * 监听批量任务完成事件，释放信号量
     */
    @EventListener
    public void onBatchJobCompleted(AsyncTaskService.BatchJobCompletedEvent event) {
        activeJobs.remove(event.taskId);
        batchSemaphore.release();
        log.info("批量任务完成，释放信号量: taskId={}", event.taskId);
    }

    private static class BatchJob {
        Long taskId;
        String type;
        int total;

        BatchJob(Long taskId, String type, int total) {
            this.taskId = taskId;
            this.type = type;
            this.total = total;
        }
    }
}
