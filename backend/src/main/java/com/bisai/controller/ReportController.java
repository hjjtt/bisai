package com.bisai.controller;

import com.bisai.common.Result;
import com.bisai.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Value("${file.upload-path}")
    private String uploadPath;

    /**
     * 导出学生个人报告
     */
    @PostMapping("/student/{submissionId}")
    public Result<Map<String, Object>> exportStudentReport(
            @PathVariable Long submissionId,
            @RequestBody Map<String, String> body) {
        String format = body.getOrDefault("format", "PDF");
        return reportService.exportStudentReport(submissionId, format);
    }

    /**
     * 导出班级报告
     */
    @PostMapping("/class/{taskId}")
    public Result<Map<String, Object>> exportClassReport(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> body) {
        String format = body.getOrDefault("format", "EXCEL");
        return reportService.exportClassReport(taskId, format);
    }

    /**
     * 下载报告文件
     */
    @GetMapping("/download/report/{fileName}")
    public ResponseEntity<Resource> downloadReport(@PathVariable String fileName) {
        Path filePath = Path.of(uploadPath, "reports", fileName);
        if (!filePath.toFile().exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(filePath);
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"")
                .body(resource);
    }
}
