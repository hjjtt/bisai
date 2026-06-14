package com.bisai.service;

import com.bisai.dto.DashboardStats;
import com.bisai.entity.*;
import com.bisai.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final UserMapper userMapper;
    private final ClassMapper classMapper;
    private final CourseMapper courseMapper;
    private final TrainingTaskMapper taskMapper;
    private final SubmissionMapper submissionMapper;
    private final MessageMapper messageMapper;
    private final CheckResultMapper checkResultMapper;
    private final AsyncTaskMapper asyncTaskMapper;
    private final AiCallLogMapper aiCallLogMapper;
    private final ScoreConsistencyService scoreConsistencyService;

    public DashboardStats.StudentStats getStudentStats(Long userId) {
        DashboardStats.StudentStats stats = new DashboardStats.StudentStats();

        // 获取学生所在班级的课程列表，用于过滤任务（与 TaskService.listTasks 保持一致）
        List<Long> classCourseIds = null;
        User student = userMapper.selectById(userId);
        if (student != null && student.getClassId() != null) {
            classCourseIds = courseMapper.selectList(
                    new LambdaQueryWrapper<Course>().eq(Course::getClassId, student.getClassId())
            ).stream().map(Course::getId).collect(Collectors.toList());
        }
        boolean hasCourses = classCourseIds != null && !classCourseIds.isEmpty();

        // 进行中的任务数（仅统计班级关联课程下的任务）
        LambdaQueryWrapper<TrainingTask> publishedWrapper = new LambdaQueryWrapper<TrainingTask>()
                .eq(TrainingTask::getStatus, "PUBLISHED");
        if (hasCourses) {
            publishedWrapper.in(TrainingTask::getCourseId, classCourseIds);
        }
        Long ongoingTasks = taskMapper.selectCount(publishedWrapper);
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

        // 近期任务（仅班级关联课程下已发布的）
        LambdaQueryWrapper<TrainingTask> recentWrapper = new LambdaQueryWrapper<TrainingTask>()
                .eq(TrainingTask::getStatus, "PUBLISHED");
        if (hasCourses) {
            recentWrapper.in(TrainingTask::getCourseId, classCourseIds);
        }
        recentWrapper.orderByDesc(TrainingTask::getEndTime).last("LIMIT 10");
        List<TrainingTask> tasks = taskMapper.selectList(recentWrapper);

        // 批量查询课程名称，避免 N+1 查询
        Set<Long> courseIds = tasks.stream()
                .map(TrainingTask::getCourseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, String> courseNameMap = new HashMap<>();
        if (!courseIds.isEmpty()) {
            courseMapper.selectList(new LambdaQueryWrapper<Course>().in(Course::getId, courseIds)).forEach(c ->
                    courseNameMap.put(c.getId(), c.getName())
            );
        }

        // 批量查询学生对这些任务的提交，避免 N+1
        List<Long> taskIds = tasks.stream().map(TrainingTask::getId).collect(Collectors.toList());
        Map<Long, Submission> taskSubmissionMap = new HashMap<>();
        if (!taskIds.isEmpty()) {
            submissionMapper.selectList(
                    new LambdaQueryWrapper<Submission>()
                            .in(Submission::getTaskId, taskIds)
                            .eq(Submission::getStudentId, userId)
                            .orderByDesc(Submission::getVersion)
            ).forEach(s -> taskSubmissionMap.putIfAbsent(s.getTaskId(), s));
        }

        List<Map<String, Object>> recentTasks = new ArrayList<>();
        for (TrainingTask task : tasks) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", task.getId());
            item.put("title", task.getTitle());
            item.put("endTime", task.getEndTime());
            item.put("courseName", courseNameMap.getOrDefault(task.getCourseId(), ""));

            Submission sub = taskSubmissionMap.get(task.getId());
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
            return emptyTeacherStats(stats);
        }

        // 教师的任务
        List<TrainingTask> tasks = taskMapper.selectList(
                new LambdaQueryWrapper<TrainingTask>().in(TrainingTask::getCourseId, courseIds)
        );
        List<Long> taskIds = tasks.stream().map(TrainingTask::getId).collect(Collectors.toList());

        if (taskIds.isEmpty()) {
            return emptyTeacherStats(stats);
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
        stats.setHighRisk(highRiskSubmissionIds.size());

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
        if (!highRiskSubmissionIds.isEmpty()) {
            List<Submission> highRiskSubs = submissionMapper.selectList(new LambdaQueryWrapper<Submission>().in(Submission::getId, highRiskSubmissionIds));
            Map<Long, CheckResult> crMap = highRiskResults.stream()
                    .collect(Collectors.toMap(CheckResult::getSubmissionId, cr -> cr, (a, b) -> a));
            List<Map<String, Object>> baseList = buildSubmissionList(highRiskSubs);
            for (Map<String, Object> item : baseList) {
                CheckResult cr = crMap.get((Long) item.get("id"));
                item.put("riskReason", cr != null ? cr.getDescription() : "");
            }
            stats.setHighRiskSubmissions(baseList);
        } else {
            stats.setHighRiskSubmissions(List.of());
        }

        return stats;
    }

    public DashboardStats.AdminStats getAdminStats(int days) {
        DashboardStats.AdminStats stats = new DashboardStats.AdminStats();

        // 基础统计
        long userCount = userMapper.selectCount(null);
        long classCount = classMapper.selectCount(null);
        long courseCount = courseMapper.selectCount(null);
        long taskCount = asyncTaskMapper.selectCount(null);
        long submissionCount = submissionMapper.selectCount(null);
        stats.setUserCount(userCount);
        stats.setClassCount(classCount);
        stats.setCourseCount(courseCount);
        stats.setTaskCount(taskCount);
        stats.setSubmissionCount(submissionCount);

        // 今日异常（最近24小时的失败提交）
        LocalDateTime yesterday = LocalDateTime.now().minusDays(1);
        long todayError = submissionMapper.selectCount(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getParseStatus, "FAILED")
                        .ge(Submission::getCreatedAt, yesterday)
        );
        stats.setTodayError(todayError);

        // 趋势计算：对比最近7天 vs 之前7天的增量百分比
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDateTime fourteenDaysAgo = LocalDateTime.now().minusDays(14);

        long recentUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().ge(User::getCreatedAt, sevenDaysAgo));
        long prevUsers = userMapper.selectCount(new LambdaQueryWrapper<User>().ge(User::getCreatedAt, fourteenDaysAgo).lt(User::getCreatedAt, sevenDaysAgo));
        stats.setUserTrend(calcTrend(recentUsers, prevUsers));

        long recentClasses = classMapper.selectCount(new LambdaQueryWrapper<ClassEntity>().ge(ClassEntity::getCreatedAt, sevenDaysAgo));
        long prevClasses = classMapper.selectCount(new LambdaQueryWrapper<ClassEntity>().ge(ClassEntity::getCreatedAt, fourteenDaysAgo).lt(ClassEntity::getCreatedAt, sevenDaysAgo));
        stats.setClassTrend(calcTrend(recentClasses, prevClasses));

        long recentTasks = asyncTaskMapper.selectCount(new LambdaQueryWrapper<AsyncTask>().ge(AsyncTask::getCreatedAt, sevenDaysAgo));
        long prevTasks = asyncTaskMapper.selectCount(new LambdaQueryWrapper<AsyncTask>().ge(AsyncTask::getCreatedAt, fourteenDaysAgo).lt(AsyncTask::getCreatedAt, sevenDaysAgo));
        stats.setTaskTrend(calcTrend(recentTasks, prevTasks));

        long recentErrors = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>().eq(Submission::getParseStatus, "FAILED").ge(Submission::getCreatedAt, sevenDaysAgo));
        long prevErrors = submissionMapper.selectCount(new LambdaQueryWrapper<Submission>().eq(Submission::getParseStatus, "FAILED").ge(Submission::getCreatedAt, fourteenDaysAgo).lt(Submission::getCreatedAt, sevenDaysAgo));
        stats.setErrorTrend(calcTrend(recentErrors, prevErrors));

        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();

        // 系统状态（真实检测）
        List<Map<String, Object>> statusList = new ArrayList<>();

        // 数据库：尝试查询验证连通性
        try {
            userMapper.selectCount(null);
            statusList.add(buildStatus("数据库服务", "运行正常"));
        } catch (Exception e) {
            statusList.add(buildErrorStatus("数据库服务", "连接异常"));
        }

        // AI 模型：根据今日调用成功率判断
        long aiTotalToday = aiCallLogMapper.selectCount(
                new LambdaQueryWrapper<AiCallLog>().ge(AiCallLog::getCreatedAt, todayStart));
        if (aiTotalToday == 0) {
            statusList.add(buildStatus("AI 模型服务", "Qwen3.5-35B（今日无调用）"));
        } else {
            long aiSuccessToday = aiCallLogMapper.selectCount(
                    new LambdaQueryWrapper<AiCallLog>().ge(AiCallLog::getCreatedAt, todayStart).eq(AiCallLog::getSuccess, true));
            double aiRate = (double) aiSuccessToday / aiTotalToday * 100;
            if (aiRate >= 90) {
                statusList.add(buildStatus("AI 模型服务", String.format("Qwen3.5-35B（成功率 %.0f%%）", aiRate)));
            } else {
                statusList.add(buildErrorStatus("AI 模型服务", String.format("成功率 %.0f%%", aiRate)));
            }
        }

        // 文件存储
        String uploadPath = System.getProperty("file.upload-path", "./data/files/");
        java.io.File uploadDir = new java.io.File(uploadPath);
        if (uploadDir.exists() && uploadDir.canWrite()) {
            statusList.add(buildStatus("文件存储", "正常运行"));
        } else {
            statusList.add(buildErrorStatus("文件存储", "目录不可用"));
        }

        // 异步任务队列
        long runningTasks = asyncTaskMapper.selectCount(
                new LambdaQueryWrapper<AsyncTask>().eq(AsyncTask::getStatus, "RUNNING"));
        long pendingTasks = asyncTaskMapper.selectCount(
                new LambdaQueryWrapper<AsyncTask>().eq(AsyncTask::getStatus, "PENDING"));
        statusList.add(buildStatus("异步任务队列", String.format("运行中（%d 运行 / %d 排队）", runningTasks, pendingTasks)));

        stats.setSystemStatus(statusList);

        // API 用量：今日已用 Token / 每日配额(200000)
        long todayTokens = aiCallLogMapper.sumTotalTokens(todayStart, LocalDateTime.now());
        long dailyTokenLimit = 200000L;
        stats.setApiUsage(Math.min(100, Math.round((double) todayTokens / dailyTokenLimit * 10000.0) / 100.0));

        // 服务器负载：今日失败调用数 / 今日总调用数
        long totalCalls = aiCallLogMapper.selectCount(
                new LambdaQueryWrapper<AiCallLog>().ge(AiCallLog::getCreatedAt, todayStart));
        long failedCalls = aiCallLogMapper.selectCount(
                new LambdaQueryWrapper<AiCallLog>().ge(AiCallLog::getCreatedAt, todayStart).eq(AiCallLog::getSuccess, false));
        stats.setServerLoad(totalCalls == 0 ? 0 : Math.min(100, Math.round((double) failedCalls / totalCalls * 10000.0) / 100.0));

        // 最近 N 天图表数据：3 次 GROUP BY 聚合查询替代 days*3 次 selectCount
        // 给 days 加上限保护，避免前端传入超大值拖垮数据库
        int safeDays = Math.max(1, Math.min(days, 90));
        LocalDateTime rangeStart = LocalDateTime.now().minusDays(safeDays - 1L).toLocalDate().atStartOfDay();
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd");
        java.time.format.DateTimeFormatter labelFmt = java.time.format.DateTimeFormatter.ofPattern("MM-dd");

        // 一次查出每天提交数
        java.util.Map<String, Long> submissionByDay = countByDay(
                submissionMapper.selectMaps(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Submission>()
                                .select("DATE_FORMAT(created_at, '%Y-%m-%d') as d, count(*) as c")
                                .ge("created_at", rangeStart)
                                .groupBy("DATE_FORMAT(created_at, '%Y-%m-%d')")
                                .orderByAsc("d")),
                "d", "c");

        // 一次查出每天解析成功数
        java.util.Map<String, Long> parsedByDay = countByDay(
                submissionMapper.selectMaps(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Submission>()
                                .select("DATE_FORMAT(created_at, '%Y-%m-%d') as d, count(*) as c")
                                .ge("created_at", rangeStart)
                                .eq("parse_status", "SUCCESS")
                                .groupBy("DATE_FORMAT(created_at, '%Y-%m-%d')")
                                .orderByAsc("d")),
                "d", "c");

        // 一次查出每天评分完成数
        java.util.Map<String, Long> scoredByDay = countByDay(
                submissionMapper.selectMaps(
                        new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Submission>()
                                .select("DATE_FORMAT(created_at, '%Y-%m-%d') as d, count(*) as c")
                                .ge("created_at", rangeStart)
                                .in("score_status", "AI_SCORED", "TEACHER_CONFIRMED", "PUBLISHED")
                                .groupBy("DATE_FORMAT(created_at, '%Y-%m-%d')")
                                .orderByAsc("d")),
                "d", "c");

        List<String> dates = new ArrayList<>();
        List<Long> submissionsPerDay = new ArrayList<>();
        List<Long> parsedPerDay = new ArrayList<>();
        List<Long> scoredPerDay = new ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        for (int i = safeDays - 1; i >= 0; i--) {
            String dayKey = today.minusDays(i).format(fmt);
            dates.add(today.minusDays(i).format(labelFmt));
            submissionsPerDay.add(submissionByDay.getOrDefault(dayKey, 0L));
            parsedPerDay.add(parsedByDay.getOrDefault(dayKey, 0L));
            scoredPerDay.add(scoredByDay.getOrDefault(dayKey, 0L));
        }

        stats.setDates(dates);
        stats.setSubmissions(submissionsPerDay);
        stats.setParsed(parsedPerDay);
        stats.setScored(scoredPerDay);

        // 最近操作日志
        stats.setRecentLogs(List.of());

        // P2.9: 评分一致性看板数据
        try {
            stats.setConsistency(scoreConsistencyService.getConsistencyTrend(30));
        } catch (Exception e) {
            log.warn("获取一致性看板数据失败: {}", e.getMessage());
        }

        return stats;
    }

    private double calcTrend(long current, long previous) {
        if (previous == 0) return current > 0 ? 100 : 0;
        return Math.round((double)(current - previous) / previous * 10000.0) / 100.0;
    }

    /**
     * 将 selectMaps 的 GROUP BY 结果（[{d: "2026-06-01", c: 5}, ...]）转为 {日期: 计数} Map。
     * 容错处理：跳过 key 缺失或计数无法解析的行。
     */
    private java.util.Map<String, Long> countByDay(List<Map<String, Object>> rows, String dayKey, String countKey) {
        java.util.Map<String, Long> result = new java.util.HashMap<>();
        if (rows == null) return result;
        for (Map<String, Object> row : rows) {
            Object d = row.get(dayKey);
            Object c = row.get(countKey);
            if (d == null) continue;
            try {
                long count = c == null ? 0L : Long.parseLong(c.toString());
                result.put(d.toString(), count);
            } catch (NumberFormatException ignored) {
            }
        }
        return result;
    }

    private Map<String, Object> buildStatus(String name, String text) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("type", "success");
        m.put("text", text);
        return m;
    }

    private Map<String, Object> buildErrorStatus(String name, String text) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("type", "danger");
        m.put("text", text);
        return m;
    }

    private List<Map<String, Object>> buildSubmissionList(List<Submission> subs) {
        if (subs.isEmpty()) return List.of();

        Set<Long> studentIds = subs.stream().map(Submission::getStudentId).filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> taskIds = subs.stream().map(Submission::getTaskId).filter(Objects::nonNull).collect(Collectors.toSet());

        Map<Long, User> studentMap = studentIds.isEmpty() ? Map.of() :
                userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, studentIds)).stream().collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, TrainingTask> taskMap = taskIds.isEmpty() ? Map.of() :
                taskMapper.selectList(new LambdaQueryWrapper<TrainingTask>().in(TrainingTask::getId, taskIds)).stream().collect(Collectors.toMap(TrainingTask::getId, t -> t));

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

    private DashboardStats.TeacherStats emptyTeacherStats(DashboardStats.TeacherStats stats) {
        stats.setPendingScore(0L);
        stats.setPendingReview(0L);
        stats.setHighRisk(0L);
        stats.setCompleted(0L);
        stats.setPendingReviews(Collections.emptyList());
        stats.setHighRiskSubmissions(Collections.emptyList());
        return stats;
    }
}
