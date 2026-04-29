package com.bisai.controller;

import com.bisai.common.Result;
import com.bisai.entity.FileEntity;
import com.bisai.mapper.FileMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileMapper fileMapper;

    @GetMapping("/{fileId}/preview")
    public ResponseEntity<Resource> preview(@PathVariable Long fileId) {
        FileEntity fileEntity = fileMapper.selectById(fileId);
        if (fileEntity == null) {
            return ResponseEntity.notFound().build();
        }

        Path path = Path.of(fileEntity.getFilePath());
        if (!path.toFile().exists()) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(path);
        String contentType = getContentType(fileEntity.getFileType());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" +
                        URLEncoder.encode(fileEntity.getOriginalName(), StandardCharsets.UTF_8) + "\"")
                .body(resource);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long fileId) {
        FileEntity fileEntity = fileMapper.selectById(fileId);
        if (fileEntity == null) {
            return ResponseEntity.notFound().build();
        }

        Path path = Path.of(fileEntity.getFilePath());
        if (!path.toFile().exists()) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                        URLEncoder.encode(fileEntity.getOriginalName(), StandardCharsets.UTF_8) + "\"")
                .body(resource);
    }

    private String getContentType(String fileType) {
        String type = fileType.toUpperCase();
        if ("PDF".equals(type)) return "application/pdf";
        if ("JPG".equals(type) || "JPEG".equals(type)) return "image/jpeg";
        if ("PNG".equals(type)) return "image/png";
        if ("DOC".equals(type) || "DOCX".equals(type)) return "application/msword";
        if ("XLS".equals(type) || "XLSX".equals(type)) return "application/vnd.ms-excel";
        if ("ZIP".equals(type)) return "application/zip";
        return "application/octet-stream";
    }
}
