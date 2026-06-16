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

        Path path = Path.of(fileEntity.getFilePath()).normalize();
        Path basePath = Path.of("./data/files/").normalize().toAbsolutePath();
        if (!path.isAbsolute()) {
            path = basePath.resolve(path).normalize();
        }
        if (!path.toAbsolutePath().startsWith(basePath)) {
            log.warn("文件路径遍历攻击检测: fileId={}, path={}", fileId, fileEntity.getFilePath());
            return ResponseEntity.status(403).build();
        }
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

        Path path = Path.of(fileEntity.getFilePath()).normalize();
        Path basePath = Path.of("./data/files/").normalize().toAbsolutePath();
        if (!path.isAbsolute()) {
            path = basePath.resolve(path).normalize();
        }
        if (!path.toAbsolutePath().startsWith(basePath)) {
            log.warn("文件路径遍历攻击检测: fileId={}, path={}", fileId, fileEntity.getFilePath());
            return ResponseEntity.status(403).build();
        }
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
            log.warn("Office文件转PDF失败 fileId={} type={}: {}", fileEntity.getId(), fileEntity.getFileType(), e.getMessage(), e);
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

    /**
     * 清洗文本：PDFBox showText 对控制字符/特殊符号会抛 IllegalArgumentException，
     * 移除 ASCII 控制字符（保留 \t），并将无法显示的字符替换为空格。
     */
    private String sanitizeText(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // 保留制表符、换行不在此处理（外部按行分割），移除其他控制字符
            if (c == '\t' || c == '\r' || c == '\f') {
                continue;
            }
            // ASCII 控制字符（0x00-0x1F）一律移除
            if (c < 0x20) {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 按可显示宽度截断行，避免超长行超出页面宽度导致渲染异常。
     * 中文字符按 2 个单位宽度估算，英文按 1。
     */
    private String truncateByWidth(String s, int maxWidthUnits) {
        if (s == null) return "";
        int width = 0;
        int end = s.length();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            width += (c > 0x7F) ? 2 : 1;
            if (width > maxWidthUnits) {
                end = i;
                break;
            }
        }
        return s.substring(0, end);
    }

    /**
     * 将多行文本渲染到 PDF，自动分页。
     * 解决原实现"全部内容塞进单页 A4 导致越界异常"的问题。
     *
     * @param pdf      PDF 文档
     * @param font     字体
     * @param fontSize 字号
     * @param lines    要渲染的文本行
     */
    private void renderLinesToPdf(org.apache.pdfbox.pdmodel.PDDocument pdf,
                                   org.apache.pdfbox.pdmodel.font.PDFont font,
                                   float fontSize, java.util.List<String> lines) throws Exception {
        org.apache.pdfbox.pdmodel.common.PDRectangle pageSize = org.apache.pdfbox.pdmodel.common.PDRectangle.A4;
        float margin = 40f;
        float lineGap = fontSize * 1.4f;          // 行距
        float startY = pageSize.getHeight() - margin;
        float bottomLimit = margin;               // 页面底部边界
        // A4 宽度 595pt，减去左右边距，按字号估算每行可容纳的宽度单位
        int maxWidthUnits = (int) ((pageSize.getWidth() - margin * 2) / (fontSize * 0.5));

        float y = startY;
        org.apache.pdfbox.pdmodel.PDPage page = new org.apache.pdfbox.pdmodel.PDPage(pageSize);
        pdf.addPage(page);
        org.apache.pdfbox.pdmodel.PDPageContentStream cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page);
        cs.setFont(font, fontSize);
        boolean inText = false;

        for (String rawLine : lines) {
            String line = sanitizeText(rawLine);
            line = truncateByWidth(line, maxWidthUnits);
            if (line.isEmpty()) {
                line = " "; // 空行占位，保持段落间距
            }

            // 超出底部边界则分页
            if (y < bottomLimit) {
                if (inText) {
                    cs.endText();
                    inText = false;
                }
                cs.close();
                page = new org.apache.pdfbox.pdmodel.PDPage(pageSize);
                pdf.addPage(page);
                cs = new org.apache.pdfbox.pdmodel.PDPageContentStream(pdf, page);
                cs.setFont(font, fontSize);
                y = startY;
            }

            if (!inText) {
                cs.beginText();
                inText = true;
            }
            cs.newLineAtOffset(margin, y);
            cs.showText(line);
            y -= lineGap;
        }
        if (inText) {
            cs.endText();
        }
        cs.close();
    }

    private void convertDocxToPdf(Path sourcePath, Path pdfPath) throws Exception {
        try (java.io.InputStream is = java.nio.file.Files.newInputStream(sourcePath);
             org.apache.poi.xwpf.usermodel.XWPFDocument doc = new org.apache.poi.xwpf.usermodel.XWPFDocument(is);
             org.apache.pdfbox.pdmodel.PDDocument pdf = new org.apache.pdfbox.pdmodel.PDDocument();
             java.io.OutputStream os = java.nio.file.Files.newOutputStream(pdfPath)) {

            org.apache.pdfbox.pdmodel.font.PDFont font = loadChineseFont(pdf);
            java.util.List<String> lines = new java.util.ArrayList<>();
            try (org.apache.poi.xwpf.extractor.XWPFWordExtractor extractor = new org.apache.poi.xwpf.extractor.XWPFWordExtractor(doc)) {
                String text = extractor.getText();
                lines.addAll(java.util.Arrays.asList(text.split("\n")));
            }
            renderLinesToPdf(pdf, font, 10f, lines);
            pdf.save(os);
        }
    }

    private void convertDocToPdf(Path sourcePath, Path pdfPath) throws Exception {
        try (java.io.InputStream is = java.nio.file.Files.newInputStream(sourcePath);
             org.apache.poi.hwpf.HWPFDocument doc = new org.apache.poi.hwpf.HWPFDocument(is);
             org.apache.pdfbox.pdmodel.PDDocument pdf = new org.apache.pdfbox.pdmodel.PDDocument();
             java.io.OutputStream os = java.nio.file.Files.newOutputStream(pdfPath)) {

            org.apache.pdfbox.pdmodel.font.PDFont font = loadChineseFont(pdf);
            java.util.List<String> lines = new java.util.ArrayList<>();
            try (org.apache.poi.hwpf.extractor.WordExtractor extractor = new org.apache.poi.hwpf.extractor.WordExtractor(doc)) {
                String text = extractor.getText();
                lines.addAll(java.util.Arrays.asList(text.split("\n")));
            }
            renderLinesToPdf(pdf, font, 10f, lines);
            pdf.save(os);
        }
    }

    private void convertExcelToPdf(Path sourcePath, Path pdfPath) throws Exception {
        try (java.io.InputStream is = java.nio.file.Files.newInputStream(sourcePath);
             org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(is);
             org.apache.pdfbox.pdmodel.PDDocument pdf = new org.apache.pdfbox.pdmodel.PDDocument();
             java.io.OutputStream os = java.nio.file.Files.newOutputStream(pdfPath)) {

            org.apache.pdfbox.pdmodel.font.PDFont font = loadChineseFont(pdf);
            java.util.List<String> lines = new java.util.ArrayList<>();
            org.apache.poi.ss.usermodel.DataFormatter formatter = new org.apache.poi.ss.usermodel.DataFormatter();
            for (org.apache.poi.ss.usermodel.Sheet sheet : workbook) {
                lines.add("【" + sheet.getSheetName() + "】");
                for (org.apache.poi.ss.usermodel.Row row : sheet) {
                    StringBuilder line = new StringBuilder();
                    for (org.apache.poi.ss.usermodel.Cell cell : row) {
                        line.append(formatter.formatCellValue(cell)).append("\t");
                    }
                    lines.add(line.toString());
                }
                lines.add("");
            }
            renderLinesToPdf(pdf, font, 8f, lines);
            pdf.save(os);
        }
    }
}
