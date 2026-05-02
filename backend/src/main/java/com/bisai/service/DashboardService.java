package com.bisai.service;

import com.bisai.dto.DashboardStats;
import com.bisai.entity.*;
import com.bisai.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserMapper userMapper;
    private final ClassMapper classMapper;
    private final CourseMapper courseMapper;
    private final TrainingTaskMapper taskMapper;
    private final SubmissionMapper submissionMapper;
    private final MessageMapper messageMapper;
    private final OperationLogMapper operationLogMapper;
    private final CheckResultMapper checkResultMapper;

    public DashboardStats.StudentStats getStudentStats(Long userId) {
        DashboardStats.StudentStats stats = new DashboardStats.StudentStats();

        // 进行中的任务数
        Long ongoingTasks = taskMapper.selectCount(
                new LambdaQueryWrapper<TrainingTask>().eq(TrainingTask::getStatus, "PUBLISHED")
        );
        stats.setOngoingTasks(ongoingTasks);

        // 已提交数
        Long submittedCount = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>().eq(Submission::getStudentId, userId)
        );
        stats.setSubmittedCount(submittedCount);

        // 待评价反馈（已提交但未发布成绩的）
        Long pendingFeedback = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getStudentId, userId)
                        .ne(Submission::getScoreStatus, "PUBLISHED")
                        .ne(Submission::getScoreStatus, "NOT_SCORED")
        );
        stats.setPendingFeedback(pendingFeedback);

        // 未读消息
        Long unreadMessages = messageMapper.selectCount(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getUserId, userId)
                        .eq(Message::getIsRead, false)
        );
        stats.setUnreadMessages(unreadMessages);

        // 近期任务（已发布的）
        List<TrainingTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<TrainingTask>()
                        .eq(TrainingTask::getStatus, "PUBLISHED")
                        .orderByDesc(TrainingTask::getEndTime)
                        .last("LIMIT 10")
        );

        // 批量查询课程名称，避免 N+1 查询
        Set<Long> courseIds = tasks.stream()
                .map(TrainingTask::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> courseNameMap = new HashMap<>();
        if (!courseIds.isEmpty()) {
            courseMapper.selectBatchIds(courseIds).forEach(c ->
                    courseNameMap.put(c.getId(), c.getName())
            );
        }

        List<Map<String, Object>> recentTasks = new ArrayList<>();
        for (TrainingTask task : tasks) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", task.getId());
            item.put("title", task.getTitle());
            item.put("endTime", task.getEndTime());
            // 添加课程名称
            item.put("courseName", courseNameMap.getOrDefault(task.getCourseId(), ""));

            // 查该学生对此任务的提交状态
            Submission sub = submissionMapper.selectOne(
                    new LambdaQueryWrapper<Submission>()
                            .eq(Submission::getTaskId, task.getId())
                            .eq(Submission::getStudentId, userId)
                            .orderByDesc(Submission::getVersion)
                            .last("LIMIT 1")
            );

            if (sub == null) {
                item.put("submitStatus", "未提交");
                item.put("score", null);
            } else {
                item.put("submitStatus", "已提交");
                item.put("score", sub.getTotalScore());
            }

            recentTasks.add(item);
        }
        stats.setRecentTasks(recentTasks);

        return stats;
    }

    public DashboardStats.TeacherStats getTeacherStats(Long userId) {
        DashboardStats.TeacherStats stats = new DashboardStats.TeacherStats();

        // 教师的课程
        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>().eq(Course::getTeacherId, userId)
        );
        List<Long> courseIds = courses.stream().map(Course::getId).collect(Collectors.toList());

        if (courseIds.isEmpty()) {
            stats.setPendingScore(0L);
            stats.setPendingReview(0L);
            stats.setHighRisk(0L);
            stats.setCompleted(0L);
            stats.setPendingReviews(Collections.emptyList());
            stats.setHighRiskSubmissions(Collections.emptyList());
            return stats;
        }

        // 教师的任务
        List<TrainingTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<TrainingTask>().in(TrainingTask::getCourseId, courseIds)
        );
        List<Long> taskIds = tasks.stream().map(TrainingTask::getId).collect(Collectors.toList());

        if (taskIds.isEmpty()) {
            stats.setPendingScore(0L);
            stats.setPendingReview(0L);
            stats.setHighRisk(0L);
            stats.setCompleted(0L);
            stats.setPendingReviews(Collections.emptyList());
            stats.setHighRiskSubmissions(Collections.emptyList());
            return stats;
        }

        // 待评价（未评分的提交）
        Long pendingScore = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .in(Submission::getTaskId, taskIds)
                        .eq(Submission::getScoreStatus, "NOT_SCORED")
        );
        stats.setPendingScore(pendingScore);

        // 待复核（AI已评分，等教师确认）
        Long pendingReview = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .in(Submission::getTaskId, taskIds)
                        .eq(Submission::getScoreStatus, "AI_SCORED")
        );
        stats.setPendingReview(pendingReview);

        // 高风险（核查结果中有 HIGH 的）
        List<CheckResult> highRiskResults = checkResultMapper.selectList(
                new LambdaQueryWrapper<CheckResult>().eq(CheckResult::getRiskLevel, "HIGH")
        );
        Set<Long> highRiskSubmissionIds = highRiskResults.stream()
                .map(CheckResult::getSubmissionId).collect(Collectors.toSet());
        stats.setHighRisk((long) highRiskSubmissionIds.size());

        // 已完成
        Long completed = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .in(Submission::getTaskId, taskIds)
                        .eq(Submission::getScoreStatus, "PUBLISHED")
        );
        stats.setCompleted(completed);

        // 待复核列表
        List<Submission> pendingSubs = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .in(Submission::getTaskId, taskIds)
                        .eq(Submission::getScoreStatus, "AI_SCORED")
                        .orderByDesc(Submission::getSubmitTime)
                        .last("LIMIT 10")
        );
        List<Map<String, Object>> pendingReviews = buildSubmissionList(pendingSubs);
        stats.setPendingReviews(pendingReviews);

        // 高风险列表 - 批量查询避免N+1
        List<Map<String, Object>> highRiskList = new ArrayList<>();
        if (!highRiskSubmissionIds.isEmpty()) {
            List<Submission> highRiskSubs = submissionMapper.selectBatchIds(highRiskSubmissionIds);
            Set<Long> studentIds = highRiskSubs.stream().map(Submission::getStudentId).filter(Objects::nonNull).collect(Collectors.toSet());
            Set<Long> hrTaskIds = highRiskSubs.stream().map(Submission::getTaskId).filter(Objects::nonNull).collect(Collectors.toSet());

            Map<Long, User> studentMap = studentIds.isEmpty() ? Map.of() :
                    userMapper.selectBatchIds(studentIds).stream().collect(Collectors.toMap(User::getId, u -> u));
            Map<Long, TrainingTask> taskMap = hrTaskIds.isEmpty() ? Map.of() :
                    taskMapper.selectBatchIds(hrTaskIds).stream().collect(Collectors.toMap(TrainingTask::getId, t -> t));
            Map<Long, CheckResult> crMap = highRiskResults.stream()
                    .collect(Collectors.toMap(CheckResult::getSubmissionId, cr -> cr, (a, b) -> a));

            for (Submission sub : highRiskSubs) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", sub.getId());
                User student = studentMap.get(sub.getStudentId());
                item.put("studentName", student != null ? student.getRealName() : "");
                TrainingTask task = taskMap.get(sub.getTaskId());
                item.put("title", task != null ? task.getTitle() : "");
                CheckResult cr = crMap.get(sub.getId());
                item.put("riskReason", cr != null ? cr.getDescription() : "");
                highRiskList.add(item);
            }
        }
        stats.setHighRiskSubmissions(highRiskList);

        return stats;
    }

    public DashboardStats.AdminStats getAdminStats() {
        DashboardStats.AdminStats stats = new DashboardStats.AdminStats();

        stats.setUserCount(userMapper.selectCount(null));
        stats.setClassCount(classMapper.selectCount(null));
        stats.setCourseCount(courseMapper.selectCount(null));
        stats.setTaskCount(taskMapper.selectCount(null));
        stats.setSubmissionCount(submissionMapper.selectCount(null));

        // 今日异常（最近24小时的失败提交）
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        Long todayError = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getParseStatus, "FAILED")
                        .ge(Submission::getCreatedAt, yesterday)
        );
        stats.setTodayError(todayError);

        // 最近操作日志
        List<OperationLog> logs = operationLogMapper.selectList(
                new LambdaQueryWrapper<OperationLog>()
                        .orderByDesc(OperationLog::getCreatedAt)
                        .last("LIMIT 5")
        );
        List<Map<String, Object>> recentLogs = logs.stream().map(log -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("time", log.getCreatedAt());
            m.put("user", log.getUsername());
            m.put("type", log.getAction());
            m.put("content", log.getDescription());
            return m;
        }).collect(Collectors.toList());
        stats.setRecentLogs(recentLogs);

        // 系统状态（由前端根据实际服务状态展示）
        stats.setSystemStatus(List.of());

        return stats;
    }

    private List<Map<String, Object>> buildSubmissionList(List<Submission> subs) {
        if (subs.isEmpty()) return List.of();

        Set<Long> studentIds = subs.stream().map(Submission::getStudentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> taskIds = subs.stream().map(Submission::getTaskId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, User> studentMap = studentIds.isEmpty() ? Map.of() :
                userMapper.selectBatchIds(studentIds).stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, TrainingTask> taskMap = taskIds.isEmpty() ? Map.of() :
                taskMapper.selectBatchIds(taskIds).stream().collect(Collectors.toMap(TrainingTask::getId, t -> t));

        List<Map<String, Object>> list = new ArrayList<>();
        for (Submission sub : subs) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", sub.getId());
            User student = studentMap.get(sub.getStudentId());
            item.put("studentName", student != null ? student.getRealName() : "");
            TrainingTask task = taskMap.get(sub.getTaskId());
            item.put("title", task != null ? task.getTitle() : "");
            item.put("submitTime", sub.getSubmitTime());
            list.add(item);
        }
        return list;
    }
}
