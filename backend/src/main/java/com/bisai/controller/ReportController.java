package com.bisai.controller;

import com.bisai.common.Result;
import com.bisai.service.ReportService;
import org.springframework.security.access.prepost.PreAuthorize;
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
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
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
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ResponseEntity<Resource> downloadReport(@PathVariable String fileName) {
        // 防止路径遍历攻击
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return ResponseEntity.badRequest().build();
        }

        Path baseDir = Path.of(uploadPath, "reports").toAbsolutePath().normalize();
        Path filePath = baseDir.resolve(fileName).normalize();
        if (!filePath.startsWith(baseDir)) {
            return ResponseEntity.badRequest().build();
        }

        if (!filePath.toFile().exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(filePath);
        String encodedName = URLEncoder.encode(fileName, StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(java.util.Objects.requireNonNull(MediaType.APPLICATION_OCTET_STREAM))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + encodedName + "\"")
                .body(resource);
    }
}
