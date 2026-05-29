package com.bisai.service;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.AsyncTask;
import com.bisai.entity.Course;
import com.bisai.entity.Submission;
import com.bisai.entity.TrainingTask;
import com.bisai.mapper.AsyncTaskMapper;
import com.bisai.mapper.CourseMapper;
import com.bisai.mapper.EvaluationTemplateMapper;
import com.bisai.mapper.SubmissionMapper;
import com.bisai.mapper.TrainingTaskMapper;
import com.bisai.entity.EvaluationTemplate;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskService {

    private final TrainingTaskMapper taskMapper;
    private final SubmissionMapper submissionMapper;
    private final AsyncTaskService asyncTaskService;
    private final AsyncTaskMapper asyncTaskMapper;
    private final PermissionService permissionService;
    private final CourseMapper courseMapper;
    private final EvaluationTemplateMapper evaluationTemplateMapper;

    private static final Map<Long, BatchJob> activeJobs = new ConcurrentHashMap<>();

    public Result<PageResult<TrainingTask>> listTasks(PageQuery query, Long courseId, String status, Long userId, String role) {
        Page<TrainingTask> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<TrainingTask> wrapper = new LambdaQueryWrapper<>();

        // 教师只能看自己课程下的任务
        if ("TEACHER".equals(role)) {
            wrapper.inSql(TrainingTask::getCourseId,
                    "SELECT id FROM course WHERE teacher_id = " + userId + " AND deleted = 0");
        }
        // 学生只能看已发布的任务
        if ("STUDENT".equals(role)) {
            wrapper.eq(TrainingTask::getStatus, "PUBLISHED");
        }

        if (courseId != null) {
            wrapper.eq(TrainingTask::getCourseId, courseId);
        }
        if (status != null && !status.isEmpty()) {
            wrapper.eq(TrainingTask::getStatus, status);
        }
        wrapper.orderByDesc(TrainingTask::getCreatedAt);

        Page<TrainingTask> result = taskMapper.selectPage(page, wrapper);
        List<TrainingTask> tasks = result.getRecords();
        fillCourseName(tasks);
        fillTemplateName(tasks);
        return Result.ok(new PageResult<>(tasks, result.getCurrent(), result.getSize(), result.getTotal()));
    }

    public Result<TrainingTask> getTask(Long id, Long userId, String role) {
        TrainingTask task = taskMapper.selectById(id);
        if (task == null) {
            return Result.error(40401, "任务不存在");
        }
        if (!permissionService.isAdmin(role) && !permissionService.isTeacherOwnerOfTask(id, userId)) {
            if ("STUDENT".equals(role) && !"PUBLISHED".equals(task.getStatus())) {
                return Result.error(40301, "无权访问该任务");
            }
        }
        fillCourseName(List.of(task));
        fillTemplateName(List.of(task));
        return Result.ok(task);
    }

    public Result<TrainingTask> createTask(TrainingTask task, Long userId) {
        if (task.getCourseId() != null && !permissionService.isTeacherOwnerOfCourse(task.getCourseId(), userId)) {
            return Result.error(40301, "无权在该课程下创建任务");
        }
        if (task.getStartTime() != null && task.getEndTime() != null
                && !task.getEndTime().isAfter(task.getStartTime())) {
            return Result.error(40001, "截止时间必须晚于开始时间");
        }
        task.setStatus("DRAFT");
        task.setId(null);
        task.setDeleted(null);
        taskMapper.insert(task);
        return Result.ok(task);
    }

    public Result<TrainingTask> updateTask(Long id, TrainingTask task, Long userId, String role) {
        if (!permissionService.isAdmin(role) && !permissionService.isTeacherOwnerOfTask(id, userId)) {
            return Result.error(40301, "无权操作该任务");
        }
        if (task.getStartTime() != null && task.getEndTime() != null
                && !task.getEndTime().isAfter(task.getStartTime())) {
            return Result.error(40001, "截止时间必须晚于开始时间");
        }
        task.setId(id);
        task.setCourseId(null);
        task.setStatus(null);
        task.setDeleted(null);
        taskMapper.updateById(task);
        return Result.ok(taskMapper.selectById(id));
    }

    public Result<Void> publishTask(Long id, Long userId, String role) {
        if (!permissionService.isAdmin(role) && !permissionService.isTeacherOwnerOfTask(id, userId)) {
            return Result.error(40301, "无权操作该任务");
        }
        TrainingTask task = taskMapper.selectById(id);
        if (task == null) {
            return Result.error(40401, "任务不存在");
        }
        if (!"DRAFT".equals(task.getStatus())) {
            return Result.error(40902, "只有草稿状态的任务可以发布");
        }
        if (task.getStartTime() != null && task.getEndTime() != null
                && !task.getEndTime().isAfter(task.getStartTime())) {
            return Result.error(40001, "截止时间必须晚于开始时间");
        }
        task.setStatus("PUBLISHED");
        taskMapper.updateById(task);
        return Result.ok();
    }

    public Result<Void> closeTask(Long id, Long userId, String role) {
        if (!permissionService.isAdmin(role) && !permissionService.isTeacherOwnerOfTask(id, userId)) {
            return Result.error(40301, "无权操作该任务");
        }
        TrainingTask task = taskMapper.selectById(id);
        if (task == null) {
            return Result.error(40401, "任务不存在");
        }
        task.setStatus("CLOSED");
        taskMapper.updateById(task);
        return Result.ok();
    }

    /**
     * 批量解析 - 使用异步任务队列，控制并发，DB 级去重
     */
    public Result<Map<String, Object>> batchParse(Long taskId, Long userId, String role) {
        if (!permissionService.isAdmin(role) && !permissionService.isTeacherOwnerOfTask(taskId, userId)) {
            return Result.error(40301, "无权操作该任务");
        }
        if (activeJobs.containsKey(taskId)) {
            return Result.error(40901, "该任务正在批量处理中，请稍后重试");
        }

        TrainingTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return Result.error(40401, "任务不存在");
        }

        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>().eq(Submission::getTaskId, taskId)
        );

        // DB 级去重：跳过已有 PENDING/RUNNING/RETRYING/SUCCESS 同类型任务的提交
        Set<Long> busyBizIds = asyncTaskMapper.selectList(
                new LambdaQueryWrapper<AsyncTask>()
                        .eq(AsyncTask::getTaskType, "PARSE")
                        .in(AsyncTask::getStatus, "PENDING", "RUNNING", "RETRYING", "SUCCESS")
        ).stream().map(AsyncTask::getBizId).collect(Collectors.toSet());

        List<Submission> toProcess = submissions.stream()
                .filter(sub -> !busyBizIds.contains(sub.getId()))
                .toList();

        if (toProcess.isEmpty()) {
            return Result.error(40901, "没有需要处理的提交（已全部处理或正在处理中）");
        }

        activeJobs.put(taskId, new BatchJob(taskId, "PARSE", toProcess.size()));

        int created = 0;
        for (Submission sub : toProcess) {
            sub.setParseStatus("PARSING");
            submissionMapper.updateById(sub);
            asyncTaskService.createTask("PARSE", sub.getId());
            created++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", submissions.size());
        result.put("created", created);
        result.put("skipped", submissions.size() - created);
        return Result.ok(result);
    }

    /**
     * 批量评分 - 使用异步任务队列，控制并发，DB 级去重
     */
    public Result<Map<String, Object>> batchScore(Long taskId, Long userId, String role) {
        if (!permissionService.isAdmin(role) && !permissionService.isTeacherOwnerOfTask(taskId, userId)) {
            return Result.error(40301, "无权操作该任务");
        }
        if (activeJobs.containsKey(taskId)) {
            return Result.error(40901, "该任务正在批量处理中，请稍后重试");
        }

        TrainingTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return Result.error(40401, "任务不存在");
        }

        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>().eq(Submission::getTaskId, taskId)
        );

        Set<Long> busyBizIds = asyncTaskMapper.selectList(
                new LambdaQueryWrapper<AsyncTask>()
                        .eq(AsyncTask::getTaskType, "SCORE")
                        .in(AsyncTask::getStatus, "PENDING", "RUNNING", "RETRYING", "SUCCESS")
        ).stream().map(AsyncTask::getBizId).collect(Collectors.toSet());

        List<Submission> toProcess = submissions.stream()
                .filter(sub -> !busyBizIds.contains(sub.getId()))
                .toList();

        if (toProcess.isEmpty()) {
            return Result.error(40901, "没有需要处理的提交（已全部处理或正在处理中）");
        }

        activeJobs.put(taskId, new BatchJob(taskId, "SCORE", toProcess.size()));

        int created = 0;
        for (Submission sub : toProcess) {
            sub.setScoreStatus("SCORING");
            submissionMapper.updateById(sub);
            asyncTaskService.createTask("SCORE", sub.getId());
            created++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", submissions.size());
        result.put("created", created);
        result.put("skipped", submissions.size() - created);
        return Result.ok(result);
    }

    /**
     * 批量核查 - 使用异步任务队列，控制并发，DB 级去重
     */
    public Result<Map<String, Object>> batchCheck(Long taskId, Long userId, String role) {
        if (!permissionService.isAdmin(role) && !permissionService.isTeacherOwnerOfTask(taskId, userId)) {
            return Result.error(40301, "无权操作该任务");
        }
        if (activeJobs.containsKey(taskId)) {
            return Result.error(40901, "该任务正在批量处理中，请稍后重试");
        }

        TrainingTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return Result.error(40401, "任务不存在");
        }

        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>().eq(Submission::getTaskId, taskId)
        );

        Set<Long> busyBizIds = asyncTaskMapper.selectList(
                new LambdaQueryWrapper<AsyncTask>()
                        .eq(AsyncTask::getTaskType, "CHECK")
                        .in(AsyncTask::getStatus, "PENDING", "RUNNING", "RETRYING", "SUCCESS")
        ).stream().map(AsyncTask::getBizId).collect(Collectors.toSet());

        List<Submission> toProcess = submissions.stream()
                .filter(sub -> !busyBizIds.contains(sub.getId()))
                .toList();

        if (toProcess.isEmpty()) {
            return Result.error(40901, "没有需要处理的提交（已全部处理或正在处理中）");
        }

        activeJobs.put(taskId, new BatchJob(taskId, "CHECK", toProcess.size()));

        int created = 0;
        for (Submission sub : toProcess) {
            sub.setCheckStatus("CHECKING");
            submissionMapper.updateById(sub);
            asyncTaskService.createTask("CHECK", sub.getId());
            created++;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("total", submissions.size());
        result.put("created", created);
        result.put("skipped", submissions.size() - created);
        return Result.ok(result);
    }

    /**
     * 查询批量操作进度（区分解析/核查/评分三类状态）
     */
    public Result<Map<String, Object>> getBatchProgress(Long taskId, Long userId, String role) {
        if (!permissionService.isAdmin(role) && !permissionService.isTeacherOwnerOfTask(taskId, userId)) {
            return Result.error(40301, "无权查看该任务进度");
        }
        Long total = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>().eq(Submission::getTaskId, taskId)
        );

        // 解析状态统计
        Long parsed = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getTaskId, taskId)
                        .eq(Submission::getParseStatus, "SUCCESS")
        );
        Long parseFailed = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getTaskId, taskId)
                        .likeRight(Submission::getParseStatus, "FAILED")
        );

        // 核查状态统计
        Long checked = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getTaskId, taskId)
                        .eq(Submission::getCheckStatus, "SUCCESS")
        );
        Long checkFailed = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getTaskId, taskId)
                        .likeRight(Submission::getCheckStatus, "CHECK_FAILED")
        );

        // 评分状态统计
        Long scored = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getTaskId, taskId)
                        .eq(Submission::getScoreStatus, "AI_SCORED")
                        .or().eq(Submission::getScoreStatus, "TEACHER_CONFIRMED")
                        .or().eq(Submission::getScoreStatus, "PUBLISHED")
        );
        Long scoreFailed = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getTaskId, taskId)
                        .likeRight(Submission::getScoreStatus, "SCORE_FAILED")
        );

        Map<String, Object> progress = new HashMap<>();
        progress.put("total", total);
        progress.put("parsed", parsed);
        progress.put("checked", checked);
        progress.put("scored", scored);
        progress.put("parseFailed", parseFailed);
        progress.put("checkFailed", checkFailed);
        progress.put("scoreFailed", scoreFailed);
        progress.put("totalFailed", parseFailed + checkFailed + scoreFailed);
        progress.put("running", Math.max(0, total - parsed - parseFailed));
        return Result.ok(progress);
    }

    /**
     * 监听批量任务完成事件
     */
    @EventListener
    public void onBatchJobCompleted(AsyncTaskService.BatchJobCompletedEvent event) {
        activeJobs.remove(event.taskId);
        log.info("批量任务完成: taskId={}", event.taskId);
    }

    private void fillCourseName(List<TrainingTask> tasks) {
        Set<Long> courseIds = tasks.stream()
                .map(TrainingTask::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (courseIds.isEmpty()) return;
        Map<Long, String> courseNameMap = new HashMap<>();
        courseMapper.selectList(new LambdaQueryWrapper<Course>().in(Course::getId, courseIds))
                .forEach(c -> courseNameMap.put(c.getId(), c.getName()));
        tasks.forEach(t -> t.setCourseName(courseNameMap.getOrDefault(t.getCourseId(), "")));
    }

    private void fillTemplateName(List<TrainingTask> tasks) {
        Set<Long> templateIds = tasks.stream()
                .map(TrainingTask::getTemplateId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (templateIds.isEmpty()) return;
        Map<Long, String> templateNameMap = new HashMap<>();
        evaluationTemplateMapper.selectList(new LambdaQueryWrapper<EvaluationTemplate>().in(EvaluationTemplate::getId, templateIds))
                .forEach(t -> templateNameMap.put(t.getId(), t.getName()));
        tasks.forEach(t -> t.setTemplateName(templateNameMap.getOrDefault(t.getTemplateId(), "")));
    }

    private static class BatchJob {
        BatchJob(Long taskId, String type, int total) {
        }
    }
}
