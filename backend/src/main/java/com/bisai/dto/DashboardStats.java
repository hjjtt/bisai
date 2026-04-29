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
        private long classCount;
        private long courseCount;
        private long taskCount;
        private long submissionCount;
        private long todayError;
        private List<Map<String, Object>> recentLogs;
        private List<Map<String, Object>> systemStatus;
    }
}
