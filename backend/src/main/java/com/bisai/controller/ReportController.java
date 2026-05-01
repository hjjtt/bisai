/*
 * Report Controller
 * Author: echo
 * Date: 2026-05-01
 * Description: 报表控制器，处理报表导出HTTP请求
 */

package com.bisai.controller;

import com.bisai.service.ReportService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @PostMapping("/student/{submissionId}")
    public ResponseEntity<byte[]> exportStudentReport(
            @PathVariable Long submissionId,
            @RequestBody Map<String, String> body) {
        String format = body.getOrDefault("format", "WORD");

        byte[] content = reportService.exportStudentReport(submissionId, format);

        String fileName = "学生报告_" + submissionId + getFileExtension(format);
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                .contentType(getContentType(format))
                .body(content);
    }

    @PostMapping("/class/{taskId}")
    public ResponseEntity<byte[]> exportClassReport(
            @PathVariable Long taskId,
            @RequestBody Map<String, String> body) {
        String format = body.getOrDefault("format", "EXCEL");

        byte[] content = reportService.exportClassReport(taskId, format);

        String fileName = "班级报告_" + taskId + getFileExtension(format);
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFileName + "\"; filename*=UTF-8''" + encodedFileName)
                .contentType(getContentType(format))
                .body(content);
    }

    private String getFileExtension(String format) {
        return switch (format.toUpperCase()) {
            case "WORD", "DOCX" -> ".docx";
            case "EXCEL", "XLSX" -> ".xlsx";
            case "PDF" -> ".pdf";
            default -> ".docx";
        };
    }

    private MediaType getContentType(String format) {
        return switch (format.toUpperCase()) {
            case "WORD", "DOCX" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            case "EXCEL", "XLSX" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            case "PDF" -> MediaType.APPLICATION_PDF;
            default -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
        };
    }
}
