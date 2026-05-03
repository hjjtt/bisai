package com.bisai.controller;

import com.bisai.entity.FileEntity;
import com.bisai.entity.Submission;
import com.bisai.entity.TrainingTask;
import com.bisai.entity.Course;
import com.bisai.entity.User;
import com.bisai.mapper.FileMapper;
import com.bisai.mapper.SubmissionMapper;
import com.bisai.mapper.TrainingTaskMapper;
import com.bisai.mapper.CourseMapper;
import com.bisai.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {

    private final FileMapper fileMapper;
    private final SubmissionMapper submissionMapper;
    private final TrainingTaskMapper taskMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;

    private static final ScheduledExecutorService CLEANUP_EXECUTOR = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "file-cleanup");
        t.setDaemon(true);
        return t;
    });

    @GetMapping("/{fileId}/preview")
    public ResponseEntity<Resource> preview(@PathVariable Long fileId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        FileEntity fileEntity = fileMapper.selectById(fileId);
        if (fileEntity == null) {
            return ResponseEntity.notFound().build();
        }

        // 权限校验
        if (!hasFileAccess(userId, user.getRole(), fileEntity)) {
            return ResponseEntity.status(403).build();
        }

        Path path = Path.of(fileEntity.getFilePath());
        if (!path.toFile().exists()) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(path);
        String contentTypeStr = getContentType(fileEntity.getFileType());
        String disposition = "inline";

        // Word/Excel 转换为 PDF 预览
        if (isOfficeFile(fileEntity.getFileType())) {
            Path pdfPath = convertToPdf(path, fileEntity);
            if (pdfPath != null && pdfPath.toFile().exists()) {
                resource = new FileSystemResource(pdfPath);
                contentTypeStr = "application/pdf";
            }
        }

        MediaType mediaType = MediaType.parseMediaType(java.util.Objects.requireNonNull(contentTypeStr));

        return ResponseEntity.ok()
                .contentType(java.util.Objects.requireNonNull(mediaType))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition + "; filename=\"" +
                        URLEncoder.encode(java.util.Objects.requireNonNullElse(fileEntity.getOriginalName(), "file"), StandardCharsets.UTF_8) + "\"")
                .body(resource);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<Resource> download(@PathVariable Long fileId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        User user = userMapper.selectById(userId);
        if (user == null) {
            return ResponseEntity.status(401).build();
        }

        FileEntity fileEntity = fileMapper.selectById(fileId);
        if (fileEntity == null) {
            return ResponseEntity.notFound().build();
        }

        // 权限校验
        if (!hasFileAccess(userId, user.getRole(), fileEntity)) {
            return ResponseEntity.status(403).build();
        }

        Path path = Path.of(fileEntity.getFilePath());
        if (!path.toFile().exists()) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(path);

        return ResponseEntity.ok()
                .contentType(java.util.Objects.requireNonNull(MediaType.APPLICATION_OCTET_STREAM))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                        URLEncoder.encode(java.util.Objects.requireNonNullElse(fileEntity.getOriginalName(), "file"), StandardCharsets.UTF_8) + "\"")
                .body(resource);
    }

    /**
     * 检查用户是否有权限访问指定文件
     */
    private boolean hasFileAccess(Long userId, String role, FileEntity fileEntity) {
        if ("ADMIN".equals(role)) {
            return true;
        }

        // 通过submissionId关联的文件
        if (fileEntity.getSubmissionId() != null) {
            Submission submission = submissionMapper.selectById(fileEntity.getSubmissionId());
            if (submission == null) {
                return false;
            }

            // 学生只能访问自己的文件
            if ("STUDENT".equals(role)) {
                return submission.getStudentId().equals(userId);
            }

            // 教师只能访问自己课程下的文件
            if ("TEACHER".equals(role)) {
                TrainingTask task = taskMapper.selectById(submission.getTaskId());
                if (task == null) {
                    return false;
                }
                Course course = courseMapper.selectById(task.getCourseId());
                return course != null && course.getTeacherId().equals(userId);
            }
        }

        // 知识库文件等其他类型，暂不限制（可扩展）
        return false;
    }

    private String getContentType(String fileType) {
        if (fileType == null) return "application/octet-stream";
        String type = fileType.toUpperCase();
        if ("PDF".equals(type)) return "application/pdf";
        if ("JPG".equals(type) || "JPEG".equals(type)) return "image/jpeg";
        if ("PNG".equals(type)) return "image/png";
        if ("DOC".equals(type) || "DOCX".equals(type)) return "application/msword";
        if ("XLS".equals(type) || "XLSX".equals(type)) return "application/vnd.ms-excel";
        if ("ZIP".equals(type)) return "application/zip";
        return "application/octet-stream";
    }

    private boolean isOfficeFile(String fileType) {
        if (fileType == null) return false;
        String type = fileType.toUpperCase();
        return "DOC".equals(type) || "DOCX".equals(type) || "XLS".equals(type) || "XLSX".equals(type);
    }

    /**
     * 将 Office 文件转换为 PDF（用于在线预览）
     */
    private Path convertToPdf(Path sourcePath, FileEntity fileEntity) {
        try {
            String type = fileEntity.getFileType() != null ? fileEntity.getFileType().toUpperCase() : "";
            Path tempDir = Path.of(System.getProperty("java.io.tmpdir"), "bisai-preview");
            java.nio.file.Files.createDirectories(tempDir);
            Path pdfPath = tempDir.resolve(fileEntity.getId() + ".pdf");

            if ("DOCX".equals(type)) {
                convertDocxToPdf(sourcePath, pdfPath);
            } else if ("DOC".equals(type)) {
                convertDocToPdf(sourcePath, pdfPath);
            } else if ("XLSX".equals(type) || "XLS".equals(type)) {
                convertExcelToPdf(sourcePath, pdfPath);
            }

            // 5分钟后自动清理临时PDF
            CLEANUP_EXECUTOR.schedule(() -> {
                try { java.nio.file.Files.deleteIfExists(pdfPath); }
                catch (Exception e) { log.debug("清理临时预览PDF失败: {}", e.getMessage()); }
            }, 5, TimeUnit.MINUTES);

            return pdfPath;
        } catch (Exception e) {
            log.warn("Office文件转PDF失败 fileId={}: {}", fileEntity.getId(), e.getMessage());
            return null;
        }
    }

    /**
     * 加载中文 PDF 字体
     */
    private org.apache.pdfbox.pdmodel.font.PDFont loadChineseFont(org.apache.pdfbox.pdmodel.PDDocument pdf) throws Exception {
        // 尝试常见系统字体路径
        String[] fontPaths = {
                "C:/Windows/Fonts/simsun.ttc",      // Windows 宋体
                "C:/Windows/Fonts/msyh.ttc",         // Windows 微软雅黑
                "C:/Windows/Fonts/simhei.ttf",       // Windows 黑体
                "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf", // Linux
                "/usr/share/fonts/wqy-zenhei/wqy-zenhei.ttc"               // Linux 文泉驿
        };

        for (String path : fontPaths) {
            java.io.File fontFile = new java.io.File(path);
            if (fontFile.exists()) {
                return org.apache.pdfbox.pdmodel.font.PDType0Font.load(pdf, fontFile);
            }
        }

        // 兜底：使用默认英文字体
        log.warn("未找到系统中文字体，PDF 预览中文可能显示异常");
        return new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
    }

    private void convertDocxToPdf(Path sourcePath, Path pdfPath) throws Exception {
        try (java.io.InputStream is = java.nio.file.Files.newInputStream(sourcePath);
             org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(is);
             org.apache.pdfbox.pdmodel.PDDocument pdf = new org.apache.pdfbox.pdmodel.PDDocument();
             java.io.OutputStream os = java.nio.file.Files.newOutputStream(pdfPath)) {
            
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
            pdf.addPage(page);

            org.apache.pdfbox.pdmodel.font.PDFont font = loadChineseFont(pdf);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page)) {
                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(50, 750);

                try (org.apache.poi.xwpf.extractor.XWPFWordExtractor extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(doc)) {
                    String text = extractor.getText();
                    String[] lines = text.split("\n");
                    for (String line : lines) {
                        if (line.length() > 80) line = line.substring(0, 80);
                        contentStream.showText(line);
                        contentStream.newLineAtOffset(0, -12);
                    }
                }
                contentStream.endText();
            }
            pdf.save(os);
        }
    }

    private void convertDocToPdf(Path sourcePath, Path pdfPath) throws Exception {
        try (java.io.InputStream is = java.nio.file.Files.newInputStream(sourcePath);
             org.apache.poi.hwpf.HWPFDocument doc = new org.apache.poi.hwpf.HWPFDocument(is);
             org.apache.pdfbox.pdmodel.PDDocument pdf = new org.apache.pdfbox.pdmodel.PDDocument();
             java.io.OutputStream os = java.nio.file.Files.newOutputStream(pdfPath)) {
            
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
            pdf.addPage(page);

            org.apache.pdfbox.pdmodel.font.PDFont font = loadChineseFont(pdf);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page)) {
                contentStream.beginText();
                contentStream.setFont(font, 10);
                contentStream.newLineAtOffset(50, 750);

                try (org.apache.poi.hwpf.extractor.WordExtractor extractor = new org.apache.poi.hwpf.extractor.WordExtractor(doc)) {
                    String text = extractor.getText();
                    String[] lines = text.split("\n");
                    for (String line : lines) {
                        if (line.length() > 80) line = line.substring(0, 80);
                        contentStream.showText(line);
                        contentStream.newLineAtOffset(0, -12);
                    }
                }
                contentStream.endText();
            }
            pdf.save(os);
        }
    }

    private void convertExcelToPdf(Path sourcePath, Path pdfPath) throws Exception {
        try (java.io.InputStream is = java.nio.file.Files.newInputStream(sourcePath);
             org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(is);
             org.apache.pdfbox.pdmodel.PDDocument pdf = new org.apache.pdfbox.pdmodel.PDDocument();
             java.io.OutputStream os = java.nio.file.Files.newOutputStream(pdfPath)) {
            
            org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(org.apache.pdfbox.pdmodel.common.PDRectangle.A4);
            pdf.addPage(page);

            org.apache.pdfbox.pdmodel.font.PDFont font = loadChineseFont(pdf);
            try (org.apache.pdfbox.pdmodel.PDPageContentStream contentStream = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page)) {
                contentStream.beginText();
                contentStream.setFont(font, 8);
                contentStream.newLineAtOffset(30, 770);

                org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter();
                for (org.apache.poi.ss.usermodel.Sheet sheet : workbook) {
                    contentStream.showText("【" + sheet.getSheetName() + "】");
                    contentStream.newLineAtOffset(0, -12);
                    for (org.apache.poi.ss.usermodel.Row row : sheet) {
                        StringBuilder line = new StringBuilder();
                        for (org.apache.poi.ss.usermodel.Cell cell : row) {
                            line.append(formatter.formatCellValue(cell)).append("\t");
                        }
                        contentStream.showText(line.toString());
                        contentStream.newLineAtOffset(0, -10);
                    }
                    contentStream.newLineAtOffset(0, -10);
                }
                contentStream.endText();
            }
            pdf.save(os);
        }
    }
}
