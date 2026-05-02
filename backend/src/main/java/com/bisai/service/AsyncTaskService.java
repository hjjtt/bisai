package com.bisai.service;

import com.bisai.entity.AsyncTask;
import com.bisai.entity.Submission;
import com.bisai.entity.TrainingTask;
import com.bisai.mapper.AsyncTaskMapper;
import com.bisai.mapper.SubmissionMapper;
import com.bisai.mapper.TrainingTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncTaskService {

    private final AsyncTaskMapper asyncTaskMapper;
    private final SubmissionMapper submissionMapper;
    private final AiService aiService;
    private final TrainingTaskMapper taskMapper;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * 创建异步任务
     */
    public Long createTask(String taskType, Long bizId) {
        AsyncTask task = new AsyncTask();
        task.setTaskType(taskType);
        task.setBizId(bizId);
        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setMaxRetry(3);
        task.setNextRunAt(LocalDateTime.now());
        asyncTaskMapper.insert(task);
        log.info("创建异步任务: type={}, bizId={}", taskType, bizId);
        return task.getId();
    }

    /**
     * 定时轮询执行待处理任务
     */
    @Scheduled(fixedDelay = 5000)
    public void processPendingTasks() {
        // 获取待执行的任务
        List<AsyncTask> tasks = asyncTaskMapper.selectList(
                new LambdaQueryWrapper<AsyncTask>()
                        .in(AsyncTask::getStatus, "PENDING", "RETRYING")
                        .le(AsyncTask::getNextRunAt, LocalDateTime.now())
                        .last("LIMIT 10")
        );

        for (AsyncTask task : tasks) {
            executeTask(task);
        }
    }

    /**
     * 执行单个任务
     */
    private void executeTask(AsyncTask task) {
        // 使用乐观锁防止重复执行
        AsyncTask fresh = asyncTaskMapper.selectById(task.getId());
        if (fresh == null || !"PENDING".equals(fresh.getStatus()) && !"RETRYING".equals(fresh.getStatus())) {
            return;
        }

        task.setStatus("RUNNING");
        asyncTaskMapper.updateById(task);

        try {
            switch (task.getTaskType()) {
                case "PARSE" -> aiService.doParse(task.getBizId());
                case "CHECK" -> aiService.doCheck(task.getBizId());
                case "SCORE" -> aiService.doScore(task.getBizId());
                default -> log.warn("未知任务类型: {}", task.getTaskType());
            }

            // 执行成功
            task.setStatus("SUCCESS");
            asyncTaskMapper.updateById(task);
            log.info("异步任务执行成功: id={}, type={}", task.getId(), task.getTaskType());

            // 检查批量任务是否完成
            checkBatchJobCompletion(task.getBizId());

        } catch (Exception e) {
            log.error("异步任务执行失败: id={}, type={}, error={}", task.getId(), task.getTaskType(), e.getMessage());
            handleTaskFailure(task, e.getMessage());
        }
    }

    /**
     * 处理任务失败
     */
    private void handleTaskFailure(AsyncTask task, String errorMessage) {
        task.setErrorMessage(errorMessage);
        task.setRetryCount(task.getRetryCount() + 1);

        if (task.getRetryCount() < task.getMaxRetry()) {
            // 等待重试，递增等待时间
            int delaySeconds = task.getRetryCount() * 30;
            task.setStatus("RETRYING");
            task.setNextRunAt(LocalDateTime.now().plusSeconds(delaySeconds));
            asyncTaskMapper.updateById(task);
            log.info("任务将重试: id={}, retryCount={}, nextRunAt={}", task.getId(), task.getRetryCount(), task.getNextRunAt());
        } else {
            // 重试次数用尽
            task.setStatus("FAILED");
            asyncTaskMapper.updateById(task);

            // 更新提交状态
            Submission submission = submissionMapper.selectById(task.getBizId());
            if (submission != null) {
                switch (task.getTaskType()) {
                    case "PARSE" -> submission.setParseStatus("FAILED");
                    case "CHECK" -> submission.setCheckStatus("CHECK_FAILED");
                    case "SCORE" -> submission.setScoreStatus("SCORE_FAILED");
                }
                submissionMapper.updateById(submission);
            }

            log.error("任务重试失败: id={}, type={}", task.getId(), task.getTaskType());
        }
    }

    /**
     * 查询任务状态
     */
    public AsyncTask getTaskStatus(Long taskId) {
        return asyncTaskMapper.selectById(taskId);
    }

    /**
     * 查询业务相关的所有任务
     */
    public List<AsyncTask> getTasksByBizId(Long bizId) {
        return asyncTaskMapper.selectList(
                new LambdaQueryWrapper<AsyncTask>()
                        .eq(AsyncTask::getBizId, bizId)
                        .orderByDesc(AsyncTask::getCreatedAt)
        );
    }

    /**
     * 手动重试失败任务
     */
    public boolean retryFailedTask(Long taskId) {
        AsyncTask task = asyncTaskMapper.selectById(taskId);
        if (task == null || !"FAILED".equals(task.getStatus())) {
            return false;
        }

        task.setStatus("PENDING");
        task.setRetryCount(0);
        task.setErrorMessage(null);
        task.setNextRunAt(LocalDateTime.now());
        asyncTaskMapper.updateById(task);
        return true;
    }

    /**
     * 检查批量任务是否全部完成，完成后发布事件
     */
    private void checkBatchJobCompletion(Long submissionId) {
        try {
            Submission submission = submissionMapper.selectById(submissionId);
            if (submission == null) return;

            // 检查该任务下是否还有未完成/运行中的异步任务
            Long pendingCount = asyncTaskMapper.selectCount(
                    new LambdaQueryWrapper<AsyncTask>()
                            .exists("SELECT 1 FROM submission s WHERE s.id = async_task.biz_id AND s.task_id = {0}", submission.getTaskId())
                            .in(AsyncTask::getStatus, "PENDING", "RUNNING", "RETRYING")
            );

            if (pendingCount == 0) {
                // 所有任务已完成，发布事件
                eventPublisher.publishEvent(new BatchJobCompletedEvent(this, submission.getTaskId()));
            }
        } catch (Exception e) {
            log.warn("检查批量任务完成状态失败: {}", e.getMessage());
        }
    }

    /**
     * 批量任务完成事件
     */
    public static class BatchJobCompletedEvent extends org.springframework.context.ApplicationEvent {
        public final Long taskId;
        public BatchJobCompletedEvent(Object source, Long taskId) {
            super(source);
            this.taskId = taskId;
        }
    }
}
