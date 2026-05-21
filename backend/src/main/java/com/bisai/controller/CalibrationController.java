package com.bisai.controller;

import com.bisai.common.Result;
import com.bisai.entity.ScoreCalibration;
import com.bisai.service.CalibrationService;
import com.bisai.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calibration")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
public class CalibrationController {

    private final CalibrationService calibrationService;
    private final PermissionService permissionService;

    @PostMapping
    public Result<Void> saveCalibration(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("");
        Object taskIdObj = body.get("taskId");
        Object submissionIdObj = body.get("submissionId");
        if (taskIdObj == null) return Result.error(40001, "taskId 不能为空");
        if (submissionIdObj == null) return Result.error(40001, "submissionId 不能为空");
        Long taskId = Long.valueOf(taskIdObj.toString());
        Long submissionId = Long.valueOf(submissionIdObj.toString());
        if (!permissionService.isAdmin(role)
                && !permissionService.isTeacherOwnerOfTask(taskId, userId)) {
            return Result.error(40301, "无权操作该任务的校准");
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        return calibrationService.saveCalibration(taskId, submissionId, items, userId);
    }

    @GetMapping("/task/{taskId}")
    public Result<List<ScoreCalibration>> getCalibrations(@PathVariable Long taskId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream().findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", "")).orElse("");
        if (!permissionService.isAdmin(role)
                && !permissionService.isTeacherOwnerOfTask(taskId, userId)) {
            return Result.error(40301, "无权查看该任务的校准数据");
        }
        return calibrationService.getCalibrations(taskId);
    }
}
