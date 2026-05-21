package com.bisai.controller;

import com.bisai.common.Result;
import com.bisai.dto.DashboardStats;
import com.bisai.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/student")
    @PreAuthorize("hasRole('STUDENT')")
    public Result<DashboardStats.StudentStats> studentStats(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(dashboardService.getStudentStats(userId));
    }

    @GetMapping("/teacher")
    @PreAuthorize("hasRole('TEACHER')")
    public Result<DashboardStats.TeacherStats> teacherStats(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return Result.ok(dashboardService.getTeacherStats(userId));
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<DashboardStats.AdminStats> adminStats(@RequestParam(defaultValue = "7") int days) {
        return Result.ok(dashboardService.getAdminStats(days));
    }
}
