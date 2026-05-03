package com.bisai.controller;

import com.bisai.common.Result;
import com.bisai.entity.ScoreCalibration;
import com.bisai.service.CalibrationService;
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

    @PostMapping
    public Result<Void> saveCalibration(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Object taskIdObj = body.get("taskId");
        Object submissionIdObj = body.get("submissionId");
        if (taskIdObj == null) return Result.error(40001, "taskId 不能为空");
        if (submissionIdObj == null) return Result.error(40001, "submissionId 不能为空");
        Long taskId = Long.valueOf(taskIdObj.toString());
        Long submissionId = Long.valueOf(submissionIdObj.toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        return calibrationService.saveCalibration(taskId, submissionId, items, userId);
    }

    @GetMapping("/task/{taskId}")
    public Result<List<ScoreCalibration>> getCalibrations(@PathVariable Long taskId) {
        return calibrationService.getCalibrations(taskId);
    }
}
