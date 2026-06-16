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
    public ResponseEntity<?> preview(@PathVariable Long fileId, Authentication auth) {
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

        // doc(旧格式) 用 WordToHtmlConverter 转 HTML 返回，保留格式
        if ("DOC".equalsIgnoreCase(fileEntity.getFileType())) {
            String html = convertDocToHtml(path);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_HTML)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" +
                            URLEncoder.encode(java.util.Objects.requireNonNullElse(fileEntity.getOriginalName(), "file"), StandardCharsets.UTF_8) + "\"")
                    .body(html);
        }

        FileSystemResource resource = new FileSystemResource(path);
        String contentTypeStr = getContentType(fileEntity.getFileType());
        String disposition = "inline";

        // docx/xls/xlsx 转换为 PDF 预览
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
     * 加载中文 PDF 字体。
     * 注意：.ttc 是 TrueType Collection（多字体合集），不能用 PDType0Font.load 直接加载
     * （fontbox 找不到 head 表报 IOException: 'head' table is mandatory）。
     * 必须用 TrueTypeCollection 取出其中的单字体。
     */
    private org.apache.pdfbox.pdmodel.font.PDFont loadChineseFont(org.apache.pdfbox.pdmodel.PDDocument pdf) throws Exception {
        // 尝试常见系统字体路径（优先 .ttf 单字体，避免 .ttc 合集解析）
        String[] fontPaths = {
                "C:/Windows/Fonts/simhei.ttf",       // Windows 黑体（单字体，优先）
                "C:/Windows/Fonts/simsun.ttc",       // Windows 宋体（合集）
                "C:/Windows/Fonts/msyh.ttc",         // Windows 微软雅黑（合集）
                "/usr/share/fonts/truetype/droid/DroidSansFallbackFull.ttf", // Linux
                "/usr/share/fonts/wqy-zenhei/wqy-zenhei.ttc"               // Linux 文泉驿
        };

        for (String path : fontPaths) {
            java.io.File fontFile = new java.io.File(path);
            if (!fontFile.exists()) {
                continue;
            }
            try {
                if (path.toLowerCase().endsWith(".ttc")) {
                    // .ttc 合集：PDFBox 3.x 的 TrueTypeCollection 用回调取字体
                    try (org.apache.fontbox.ttf.TrueTypeCollection ttc = new org.apache.fontbox.ttf.TrueTypeCollection(fontFile)) {
                        final org.apache.pdfbox.pdmodel.PDDocument docRef = pdf;
                        final org.apache.pdfbox.pdmodel.font.PDFont[] holder = new org.apache.pdfbox.pdmodel.font.PDFont[1];
                        ttc.processAllFonts(ttf -> {
                            if (holder[0] == null) {
                                try {
                                    holder[0] = org.apache.pdfbox.pdmodel.font.PDType0Font.load(docRef, ttf, true);
                                } catch (Exception ex) {
                                    log.warn("从 ttc 加载字体失败: {}", ex.getMessage());
                                }
                            }
                        });
                        if (holder[0] != null) {
                            return holder[0];
                        }
                    }
                } else {
                    // .ttf 单字体：直接加载
                    return org.apache.pdfbox.pdmodel.font.PDType0Font.load(pdf, fontFile);
                }
            } catch (Exception e) {
                log.warn("加载字体失败，尝试下一个: {} - {}", path, e.getMessage());
            }
        }

        // 兜底：使用默认英文字体（中文会显示为方块，但至少不抛异常）
        log.warn("未找到可用的系统中文字体，PDF 预览中文可能显示异常");
        return new org.apache.pdfbox.pdmodel.font.PDType1Font(org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
    }

    /**
     * 清洗文本：PDFBox showText 对控制字符/字体缺失字符会抛 IllegalArgumentException 或 IOException，
     * 移除 ASCII 控制字符、把特殊空白（&nbsp; 等）替换为普通空格。
     */
    private String sanitizeText(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // 制表符保留为普通空格（PDF 文本流里 \t 可能引起对齐问题）
            if (c == '\t' || c == '\r' || c == '\f') {
                continue;
            }
            // 特殊空白类字符统一替换为普通空格：U+00A0(&nbsp;)、U+2000-U+200A、U+202F、U+205F、U+3000(全角空格)、U+FEFF(BOM)
            if (c == 0x00A0 || c == 0x202F || c == 0x205F || c == 0x3000 || c == 0xFEFF
                    || (c >= 0x2000 && c <= 0x200A)) {
                sb.append(' ');
                continue;
            }
            // ASCII 控制字符（0x00-0x1F）、DEL(0x7F) 一律移除
            if (c < 0x20 || c == 0x7F) {
                continue;
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /**
     * 剔除字体不支持的字符。PDFBox 的 showText 遇到字体缺失的字符会抛
     * IOException("could not find the glyphId")，导致整个转换失败。
     *
     * 策略：只在 getStringWidth 抛异常时才丢弃字符；
     * 返回 0 宽度不等于不支持（零宽字符、字体 metrics 问题都可能返回 0），
     * 保留这些字符以避免误杀中文。
     */
    private String filterSupportedChars(String s, org.apache.pdfbox.pdmodel.font.PDFont font) {
        if (s == null || s.isEmpty()) return s;
        int dropped = 0;
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            // 普通空格一定保留（排版需要）
            if (c == ' ') {
                sb.append(c);
                continue;
            }
            try {
                // getStringWidth 对字体缺失的字符抛 IllegalArgumentException 或 IOException
                // 只要不抛异常就保留（即使宽度返回 0 也不丢弃，避免误杀中文字）
                font.getStringWidth(String.valueOf(c));
                sb.append(c);
            } catch (Exception e) {
                // 字体确实不支持该字符，丢弃
                dropped++;
            }
        }
        if (dropped > 0) {
            log.debug("filterSupportedChars: 丢弃 {} 个不支持的字符 (总 {} 个)", dropped, s.length());
        }
        return sb.toString();
    }

    /**
     * 按可显示宽度截断行（兜底用）。
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
     * 按可显示宽度将长行拆成多行（自动换行），避免内容被截断丢失。
     * 中文字符按 2 个单位宽度估算，英文按 1。
     */
    private java.util.List<String> wrapByWidth(String s, int maxWidthUnits) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (s == null || s.isEmpty()) {
            result.add("");
            return result;
        }
        int width = 0;
        int start = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            width += (c > 0x7F) ? 2 : 1;
            if (width > maxWidthUnits) {
                result.add(s.substring(start, i));
                start = i;
                width = (c > 0x7F) ? 2 : 1;
            }
        }
        if (start < s.length()) {
            result.add(s.substring(start));
        }
        return result;
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
            // 剔除字体不支持的字符，避免 showText 抛 IOException(glyphId not found)
            line = filterSupportedChars(line, font);
            // 长行自动换行（而非截断丢失内容）
            java.util.List<String> wrappedLines = wrapByWidth(line, maxWidthUnits);

            for (String subLine : wrappedLines) {
                if (subLine.isEmpty()) {
                    subLine = " "; // 空行占位，保持段落间距
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
                try {
                    cs.showText(subLine);
                } catch (Exception e) {
                    log.warn("showText 失败，跳过该行 (len={}): {}", subLine.length(), e.getMessage());
                    cs.endText();
                    inText = false;
                }
                y -= lineGap;
            }
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

            // 提取正文段落（保留表格结构）
            extractDocxContent(doc, lines);

            // 提取页眉
            for (org.apache.poi.xwpf.usermodel.XWPFHeader header : doc.getHeaderList()) {
                for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : header.getParagraphs()) {
                    String t = p.getText();
                    if (t != null && !t.isBlank()) lines.add(t);
                }
            }
            // 提取页脚
            for (org.apache.poi.xwpf.usermodel.XWPFFooter footer : doc.getFooterList()) {
                for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : footer.getParagraphs()) {
                    String t = p.getText();
                    if (t != null && !t.isBlank()) lines.add(t);
                }
            }

            log.info("DOCX→PDF 提取到 {} 行文本, source={}", lines.size(), sourcePath.getFileName());
            if (lines.isEmpty()) {
                lines.add("（文档内容为空）");
            }
            renderLinesToPdf(pdf, font, 10f, lines);
            pdf.save(os);
        }
    }

    /**
     * 递归提取 DOCX 正文内容（段落 + 表格 + 表格嵌套表格），
     * 按文档顺序输出，表格单元格用 " | " 分隔。
     */
    private void extractDocxContent(org.apache.poi.xwpf.usermodel.XWPFDocument doc, java.util.List<String> lines) {
        // XWPFDocument.getBodyElements() 按文档顺序返回段落和表格
        for (org.apache.poi.xwpf.usermodel.IBodyElement element : doc.getBodyElements()) {
            if (element.getElementType() == org.apache.poi.xwpf.usermodel.BodyElementType.PARAGRAPH) {
                org.apache.poi.xwpf.usermodel.XWPFParagraph p = (org.apache.poi.xwpf.usermodel.XWPFParagraph) element;
                String t = p.getText();
                if (t != null) lines.add(t);
            } else if (element.getElementType() == org.apache.poi.xwpf.usermodel.BodyElementType.TABLE) {
                org.apache.poi.xwpf.usermodel.XWPFTable table = (org.apache.poi.xwpf.usermodel.XWPFTable) element;
                extractDocxTable(table, lines);
            }
        }
    }

    private void extractDocxTable(org.apache.poi.xwpf.usermodel.XWPFTable table, java.util.List<String> lines) {
        for (org.apache.poi.xwpf.usermodel.XWPFTableRow row : table.getRows()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < row.getTableCells().size(); i++) {
                org.apache.poi.xwpf.usermodel.XWPFTableCell cell = row.getTableCells().get(i);
                if (i > 0) sb.append(" | ");
                // 单元格内可能有多个段落
                boolean first = true;
                for (org.apache.poi.xwpf.usermodel.XWPFParagraph p : cell.getParagraphs()) {
                    String t = p.getText();
                    if (t != null && !t.isBlank()) {
                        if (!first) sb.append(" ");
                        sb.append(t.trim());
                        first = false;
                    }
                }
            }
            if (sb.length() > 0) {
                lines.add(sb.toString());
            }
        }
        lines.add(""); // 表格后空行
    }

    private void convertDocToPdf(Path sourcePath, Path pdfPath) throws Exception {
        try (java.io.InputStream is = java.nio.file.Files.newInputStream(sourcePath);
             org.apache.poi.hwpf.HWPFDocument doc = new org.apache.poi.hwpf.HWPFDocument(is);
             org.apache.pdfbox.pdmodel.PDDocument pdf = new org.apache.pdfbox.pdmodel.PDDocument();
             java.io.OutputStream os = java.nio.file.Files.newOutputStream(pdfPath)) {

            org.apache.pdfbox.pdmodel.font.PDFont font = loadChineseFont(pdf);
            java.util.List<String> lines = new java.util.ArrayList<>();

            // WordExtractor.getText() 已经包含正文+表格文本，但也提取页眉/页脚
            try (org.apache.poi.hwpf.extractor.WordExtractor extractor = new org.apache.poi.hwpf.extractor.WordExtractor(doc)) {
                // 正文文本（含表格）
                String bodyText = extractor.getText();
                if (bodyText != null && !bodyText.isBlank()) {
                    lines.addAll(java.util.Arrays.asList(bodyText.split("\n")));
                }
                // 页眉
                String headerText = extractor.getHeaderText();
                if (headerText != null && !headerText.isBlank()) {
                    lines.addAll(java.util.Arrays.asList(headerText.split("\n")));
                }
                // 页脚
                String footerText = extractor.getFooterText();
                if (footerText != null && !footerText.isBlank()) {
                    lines.addAll(java.util.Arrays.asList(footerText.split("\n")));
                }
            }

            // 如果 WordExtractor 提取的正文为空，尝试用 Range API 递归提取
            if (lines.isEmpty()) {
                log.warn("WordExtractor 未提取到文本，尝试 Range API 提取, source={}", sourcePath.getFileName());
                extractDocRange(doc.getRange(), lines);
            }

            log.info("DOC→PDF 提取到 {} 行文本, source={}", lines.size(), sourcePath.getFileName());
            if (lines.isEmpty()) {
                lines.add("（文档内容为空或包含不支持的格式）");
            }
            renderLinesToPdf(pdf, font, 10f, lines);
            pdf.save(os);
        }
    }

    /**
     * 将 .doc 文件转换为 HTML（保留格式、表格、图片）。
     * 使用 POI 的 WordToHtmlConverter，比纯文本提取效果好得多。
     */
    private String convertDocToHtml(Path sourcePath) {
        try (java.io.InputStream is = java.nio.file.Files.newInputStream(sourcePath)) {
            org.apache.poi.hwpf.HWPFDocument doc = new org.apache.poi.hwpf.HWPFDocument(is);

            javax.xml.parsers.DocumentBuilderFactory factory = javax.xml.parsers.DocumentBuilderFactory.newInstance();
            javax.xml.parsers.DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document htmlDoc = builder.newDocument();

            org.apache.poi.hwpf.converter.WordToHtmlConverter converter =
                    new org.apache.poi.hwpf.converter.WordToHtmlConverter(htmlDoc);

            converter.processDocument(doc);

            // 将 DOM 转为 HTML 字符串
            javax.xml.transform.TransformerFactory tf = javax.xml.transform.TransformerFactory.newInstance();
            javax.xml.transform.Transformer transformer = tf.newTransformer();
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.METHOD, "html");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "no");
            transformer.setOutputProperty(javax.xml.transform.OutputKeys.ENCODING, "UTF-8");
            java.io.StringWriter sw = new java.io.StringWriter();
            transformer.transform(new javax.xml.transform.dom.DOMSource(converter.getDocument()),
                    new javax.xml.transform.stream.StreamResult(sw));
            return sw.toString();
        } catch (Exception e) {
            log.warn("DOC→HTML 转换失败: {}", e.getMessage(), e);
            return "<html><body><p style='color:red'>文档转换失败: " + e.getMessage() + "</p></body></html>";
        }
    }

    /**
     * 用 HWPF Range API 递归提取 .doc 文本（段落 + 表格）。
     * Range.numParagraphs() 遍历文档所有段落（包括表格单元格内的段落）。
     */
    private void extractDocRange(org.apache.poi.hwpf.usermodel.Range range, java.util.List<String> lines) {
        for (int i = 0; i < range.numParagraphs(); i++) {
            org.apache.poi.hwpf.usermodel.Paragraph para = range.getParagraph(i);
            String text = para.text();
            if (text != null) {
                // 去掉 HWPF 段落末尾的 \r（paragraph mark）
                text = text.endsWith("\r") ? text.substring(0, text.length() - 1) : text;
                if (!text.isBlank()) {
                    lines.add(text);
                }
            }
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
