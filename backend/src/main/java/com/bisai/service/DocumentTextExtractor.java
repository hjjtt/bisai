package com.bisai.service;

import com.bisai.entity.FileEntity;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@Service
public class DocumentTextExtractor {

    // ZIP 安全限制
    private static final long MAX_ZIP_SIZE = 500 * 1024 * 1024L; // 500MB
    private static final int MAX_ENTRY_COUNT = 1000;
    private static final int MAX_NEST_LEVEL = 3;
    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(
            "txt", "md", "csv", "java", "py", "html", "css", "js", "json", "xml", "sql",
            "pdf", "doc", "docx", "xls", "xlsx", "jpg", "jpeg", "png", "webp", "bmp"
    );

    public ExtractedText extract(FileEntity file) {
        try {
            Path path = Path.of(file.getFilePath());
            if (!Files.exists(path)) {
                return new ExtractedText("", "MISSING");
            }
            String type = normalize(file.getFileType());
            return switch (type) {
                case "TXT", "MD", "CSV", "JAVA", "PY", "HTML", "CSS", "JS", "JSON", "XML", "SQL" ->
                        new ExtractedText(Files.readString(path), "TEXT");
                case "PDF" -> new ExtractedText(readPdf(path), "PDFBOX");
                case "DOCX" -> new ExtractedText(readDocx(path), "POI-DOCX");
                case "DOC" -> new ExtractedText(readDoc(path), "POI-DOC");
                case "XLSX" -> new ExtractedText(readWorkbook(path), "POI-XLSX");
                case "XLS" -> new ExtractedText(readXls(path), "POI-XLS");
                case "JPG", "JPEG", "PNG", "WEBP", "BMP" ->
                        new ExtractedText("(图片文件: " + file.getOriginalName() + ", 等待多模态分析)", "IMAGE");
                case "ZIP" -> extractZip(path, file.getOriginalName());
                case "RAR", "7Z" ->
                        new ExtractedText("(压缩文件: " + file.getOriginalName() + "，暂不支持该格式)", "ARCHIVE");
                default -> new ExtractedText("", "UNKNOWN");
            };
        } catch (Exception e) {
            log.warn("读取文件内容失败 fileId={}, path={}", file.getId(), file.getFilePath(), e);
            return new ExtractedText("", "FAILED");
        }
    }

    public boolean isImage(FileEntity file) {
        String type = normalize(file.getFileType());
        return "JPG".equals(type) || "JPEG".equals(type) || "PNG".equals(type) || "WEBP".equals(type) || "BMP".equals(type);
    }

    /**
     * 安全解压 ZIP 文件，防止 ZIP bomb、路径穿越等攻击
     */
    private ExtractedText extractZip(Path zipPath, String originalName) {
        StringBuilder content = new StringBuilder();
        int entryCount = 0;
        long totalSize = 0;

        try (ZipInputStream zis = new ZipInputStream(Files.newInputStream(zipPath))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryCount++;
                if (entryCount > MAX_ENTRY_COUNT) {
                    content.append("\n[警告] ZIP 文件条目数超过限制(").append(MAX_ENTRY_COUNT).append(")，停止解压]");
                    break;
                }

                // 路径穿越检查
                String entryName = entry.getName();
                if (entryName.contains("..") || entryName.startsWith("/") || entryName.startsWith("\\")) {
                    content.append("\n[跳过] 发现可疑路径: ").append(entryName);
                    continue;
                }

                if (entry.isDirectory()) {
                    continue;
                }

                // 文件大小检查
                long entrySize = entry.getSize();
                if (entrySize > 0) {
                    totalSize += entrySize;
                    if (totalSize > MAX_ZIP_SIZE) {
                        content.append("\n[警告] ZIP 解压总大小超过限制(").append(MAX_ZIP_SIZE / 1024 / 1024).append("MB)，停止解压]");
                        break;
                    }
                }

                // 只处理支持的文件类型
                String ext = getExtension(entryName);
                if (!SUPPORTED_EXTENSIONS.contains(ext)) {
                    continue;
                }

                content.append("\n=== 文件: ").append(entryName).append(" ===\n");
                try {
                    byte[] data = zis.readAllBytes();
                    if (data.length > 100000) {
                        content.append(new String(data, 0, 100000)).append("\n...(内容过长已截断)");
                    } else {
                        content.append(new String(data));
                    }
                } catch (Exception e) {
                    content.append("(读取失败: ").append(e.getMessage()).append(")");
                }

                zis.closeEntry();
            }
        } catch (Exception e) {
            log.warn("ZIP 解压失败: {}", originalName, e);
            return new ExtractedText("(压缩文件解压失败: " + e.getMessage() + ")", "ARCHIVE_FAILED");
        }

        if (content.isEmpty()) {
            return new ExtractedText("(压缩文件中无支持的文件类型)", "ARCHIVE_EMPTY");
        }

        return new ExtractedText(content.toString(), "ZIP");
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private String readPdf(Path path) throws Exception {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String readDocx(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(input);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String readDoc(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path);
             HWPFDocument document = new HWPFDocument(input);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String readWorkbook(Path path) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (InputStream input = Files.newInputStream(path);
             Workbook workbook = WorkbookFactory.create(input)) {
            DataFormatter formatter = new DataFormatter();
            for (Sheet sheet : workbook) {
                builder.append("【工作表: ").append(sheet.getSheetName()).append("】\n");
                for (Row row : sheet) {
                    for (Cell cell : row) {
                        builder.append(formatter.formatCellValue(cell)).append('\t');
                    }
                    builder.append('\n');
                }
            }
        }
        return builder.toString();
    }

    private String readXls(Path path) throws Exception {
        try (InputStream input = Files.newInputStream(path);
             ExcelExtractor extractor = new ExcelExtractor(new org.apache.poi.hssf.usermodel.HSSFWorkbook(input))) {
            return extractor.getText();
        }
    }

    private String normalize(String type) {
        return type == null ? "" : type.trim().toUpperCase(Locale.ROOT);
    }

    public record ExtractedText(String content, String parserType) {
    }
}
