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
import java.util.Locale;

@Slf4j
@Service
public class DocumentTextExtractor {

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
                case "ZIP", "RAR", "7Z" ->
                        new ExtractedText("(压缩文件: " + file.getOriginalName() + "，暂未展开解析)", "ARCHIVE");
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
