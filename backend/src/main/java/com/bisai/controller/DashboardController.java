package com.bisai.controller;

import com.bisai.common.Result;
import com.bisai.dto.DashboardStats;
import com.bisai.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/student")
    public Result<DashboardStats.StudentStats> studentStats(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(dashboardService.getStudentStats(userId));
    }

    @GetMapping("/teacher")
    public Result<DashboardStats.TeacherStats> teacherStats(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(dashboardService.getTeacherStats(userId));
    }

    @GetMapping("/admin")
    public Result<DashboardStats.AdminStats> adminStats() {
        return Result.ok(dashboardService.getAdminStats());
    }
}
