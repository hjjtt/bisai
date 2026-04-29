package com.bisai.controller;

import com.bisai.common.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    /**
     * 导出学生个人报告（MVP 阶段模拟实现）
     */
    @PostMapping("/student/{submissionId}")
    public Result<Map<String, Object>> exportStudentReport(
            @PathVariable Long submissionId,
            @RequestBody Map<String, String> body) {
        String format = body.getOrDefault("format", "PDF");
        // MVP 阶段：返回模拟的文件信息
        Map<String, Object> result = new HashMap<>();
        result.put("fileId", System.currentTimeMillis());
        result.put("fileName", "学生报告_" + submissionId + "." + format.toLowerCase());
        return Result.ok(result);
    }

    /**
     * 导出班级报告（MVP 阶段模拟实现）
     */
    @PostMapping("/class/{taskId}")
    public Result<Map<String, Object>> exportClassReport(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> body) {
        String format = body.getOrDefault("format", "PDF");
        // MVP 阶段：返回模拟的文件信息
        Map<String, Object> result = new HashMap<>();
        result.put("fileId", System.currentTimeMillis());
        result.put("fileName", "班级报告_" + taskId + "." + format.toLowerCase());
        return Result.ok(result);
    }
}
