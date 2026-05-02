package com.bisai.controller;

import com.bisai.common.Result;
import com.bisai.entity.ScoreCalibration;
import com.bisai.service.CalibrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/calibration")
@RequiredArgsConstructor
public class CalibrationController {

    private final CalibrationService calibrationService;

    @PostMapping
    public Result<Void> saveCalibration(@RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Long taskId = Long.valueOf(body.get("taskId").toString());
        Long submissionId = Long.valueOf(body.get("submissionId").toString());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        return calibrationService.saveCalibration(taskId, submissionId, items, userId);
    }

    @GetMapping("/task/{taskId}")
    public Result<List<ScoreCalibration>> getCalibrations(@PathVariable Long taskId) {
        return calibrationService.getCalibrations(taskId);
    }
}
