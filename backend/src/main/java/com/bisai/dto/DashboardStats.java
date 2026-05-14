package com.bisai.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class DashboardStats {

    // 学生首页统计
    @Data
    public static class StudentStats {
        private long ongoingTasks;
        private long submittedCount;
        private long pendingFeedback;
        private long unreadMessages;
        private List<Map<String, Object>> recentTasks;
    }

    // 教师首页统计
    @Data
    public static class TeacherStats {
        private long pendingScore;
        private long pendingReview;
        private long highRisk;
        private long completed;
        private List<Map<String, Object>> pendingReviews;
        private List<Map<String, Object>> highRiskSubmissions;
    }

    // 管理员首页统计
    @Data
    public static class AdminStats {
        private long userCount;
        private double userTrend;
        private long classCount;
        private double classTrend;
        private long courseCount;
        private long taskCount;
        private double taskTrend;
        private long submissionCount;
        private long todayError;
        private double errorTrend;
        private List<Map<String, Object>> recentLogs;
        private List<Map<String, Object>> systemStatus;
        private double apiUsage;
        private double serverLoad;
        private List<String> dates;
        private List<Long> submissions;
        private List<Long> parsed;
        private List<Long> scored;
    }
}
