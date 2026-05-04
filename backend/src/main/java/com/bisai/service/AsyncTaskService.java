package com.bisai.service;

import com.bisai.entity.AsyncTask;
import com.bisai.entity.Submission;
import com.bisai.mapper.AsyncTaskMapper;
import com.bisai.mapper.SubmissionMapper;
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
        // 1. 清理僵尸任务（处于 RUNNING 状态超过 10 分钟的任务）
        cleanupStuckTasks();

        // 2. 获取待执行的任务
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
     * 清理并重置长时间卡在 RUNNING 状态的任务
     */
    private void cleanupStuckTasks() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        List<AsyncTask> stuckTasks = asyncTaskMapper.selectList(
                new LambdaQueryWrapper<AsyncTask>()
                        .eq(AsyncTask::getStatus, "RUNNING")
                        .lt(AsyncTask::getUpdatedAt, threshold)
        );

        for (AsyncTask task : stuckTasks) {
            log.warn("检测到僵尸任务，执行重置: id={}, type={}, bizId={}", task.getId(), task.getTaskType(), task.getBizId());
            handleTaskFailure(task, "任务执行超时或意外中断（僵尸任务清理）");
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

        fresh.setStatus("RUNNING");
        fresh.setProgress(5);
        fresh.setCurrentStep("任务开始执行...");
        asyncTaskMapper.updateById(fresh);

        try {
            switch (fresh.getTaskType()) {
                case "PARSE" -> aiService.doParse(fresh.getBizId(), fresh.getId());
                case "CHECK" -> aiService.doCheck(fresh.getBizId(), fresh.getId());
                case "SCORE" -> aiService.doScore(fresh.getBizId(), fresh.getId());
                default -> log.warn("未知任务类型: {}", fresh.getTaskType());
            }

            // 执行成功
            fresh.setStatus("SUCCESS");
            fresh.setProgress(100);
            fresh.setCurrentStep("执行完成");
            asyncTaskMapper.updateById(fresh);
            log.info("异步任务执行成功: id={}, type={}", fresh.getId(), fresh.getTaskType());

            // 同步更新 submission 表的状态
            Submission submission = submissionMapper.selectById(fresh.getBizId());
            if (submission != null) {
                switch (fresh.getTaskType()) {
                    case "PARSE" -> submission.setParseStatus("SUCCESS");
                    case "CHECK" -> submission.setCheckStatus("SUCCESS");
                    case "SCORE" -> submission.setScoreStatus("AI_SCORED");
                }
                submissionMapper.updateById(submission);
            }

            // 检查批量任务是否完成
            checkBatchJobCompletion(fresh.getBizId());

        } catch (Exception e) {
            log.error("异步任务执行失败: id={}, type={}, error={}", fresh.getId(), fresh.getTaskType(), e.getMessage());
            handleTaskFailure(fresh, e.getMessage());
        }
    }

    /**
     * 处理任务失败
     */
    private void handleTaskFailure(AsyncTask task, String errorMessage) {
        task.setErrorMessage(errorMessage);
        task.setRetryCount(task.getRetryCount() + 1);

        boolean isFinalFailure = task.getRetryCount() >= task.getMaxRetry();
        if (!isFinalFailure) {
            // 等待重试，递增等待时间
            int delaySeconds = task.getRetryCount() * 30;
            task.setStatus("RETRYING");
            task.setNextRunAt(LocalDateTime.now().plusSeconds(delaySeconds));
            log.info("任务将重试: id={}, retryCount={}, nextRunAt={}", task.getId(), task.getRetryCount(), task.getNextRunAt());
        } else {
            // 重试次数用尽
            task.setStatus("FAILED");
            log.error("任务重试失败，次数已达上限: id={}, type={}", task.getId(), task.getTaskType());
        }
        asyncTaskMapper.updateById(task);

        // 只要任务不再运行，就更新提交状态，释放锁定
        updateSubmissionStatusOnFailure(task);
    }

    private void updateSubmissionStatusOnFailure(AsyncTask task) {
        Submission submission = submissionMapper.selectById(task.getBizId());
        if (submission != null) {
            String statusSuffix = "RETRYING".equals(task.getStatus()) ? " (待重试)" : "";
            switch (task.getTaskType()) {
                case "PARSE" -> submission.setParseStatus("FAILED" + statusSuffix);
                case "CHECK" -> submission.setCheckStatus("CHECK_FAILED" + statusSuffix);
                case "SCORE" -> submission.setScoreStatus("SCORE_FAILED" + statusSuffix);
            }
            submissionMapper.updateById(submission);
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
     * 取消正在执行的任务
     */
    public boolean cancelTask(Long taskId) {
        AsyncTask task = asyncTaskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        if (!"PENDING".equals(task.getStatus()) && !"RUNNING".equals(task.getStatus())
                && !"RETRYING".equals(task.getStatus())) {
            return false;
        }

        task.setStatus("CANCELLED");
        task.setUpdatedAt(LocalDateTime.now());
        asyncTaskMapper.updateById(task);
        log.info("任务已取消: id={}, type={}, bizId={}", task.getId(), task.getTaskType(), task.getBizId());

        // 释放 submission 锁定状态
        updateSubmissionStatusOnCancel(task);
        return true;
    }

    private void updateSubmissionStatusOnCancel(AsyncTask task) {
        Submission submission = submissionMapper.selectById(task.getBizId());
        if (submission != null) {
            switch (task.getTaskType()) {
                case "PARSE" -> submission.setParseStatus("CANCELLED");
                case "CHECK" -> submission.setCheckStatus("CANCELLED");
                case "SCORE" -> submission.setScoreStatus("CANCELLED");
            }
            submissionMapper.updateById(submission);
        }
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
