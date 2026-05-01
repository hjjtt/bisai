/*
 * PDF Report Generator
 * Author: echo
 * Date: 2026-05-01
 * Description: PDF报告生成工具类，支持中文显示
 */

package com.bisai.util;

import com.bisai.dto.StudentReportDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
public class PdfReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final float MARGIN = 50;
    private static final float FONT_SIZE_TITLE = 18;
    private static final float FONT_SIZE_SECTION = 14;
    private static final float FONT_SIZE_NORMAL = 12;
    private static final float LINE_SPACING = 18;

    private static PDFont loadChineseFont(PDDocument document) throws IOException {
        File simheiTtf = new File("C:/Windows/Fonts/simhei.ttf");
        if (simheiTtf.exists()) {
            log.info("Loaded font: simhei.ttf");
            return PDType0Font.load(document, simheiTtf);
        }
        
        File simsunbTtf = new File("C:/Windows/Fonts/simsunb.ttf");
        if (simsunbTtf.exists()) {
            log.info("Loaded font: simsunb.ttf");
            return PDType0Font.load(document, simsunbTtf);
        }
        
        log.warn("No Chinese font found, using Helvetica");
        return new org.apache.pdfbox.pdmodel.font.PDType1Font(
                org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName.HELVETICA);
    }

    public static byte[] generateStudentReport(StudentReportDTO report) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            PDFont font = loadChineseFont(document);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - MARGIN;

                y = drawTitle(contentStream, font, "实训成果评价报告", y);
                y -= LINE_SPACING;

                y = drawSectionTitle(contentStream, font, "一、基本信息", y);
                y = drawKeyValue(contentStream, font, "学生姓名", report.getStudentName() != null ? report.getStudentName() : "未知", y);
                y = drawKeyValue(contentStream, font, "班级", report.getClassName() != null ? report.getClassName() : "未分配", y);
                y = drawKeyValue(contentStream, font, "实训任务", report.getTaskTitle() != null ? report.getTaskTitle() : "未知", y);
                y = drawKeyValue(contentStream, font, "提交时间",
                        report.getSubmitTime() != null ? report.getSubmitTime().format(DATE_FORMATTER) : "未知", y);
                y = drawKeyValue(contentStream, font, "版本号", String.valueOf(report.getVersion() != null ? report.getVersion() : 1), y);
                y -= LINE_SPACING;

                y = drawSectionTitle(contentStream, font, "二、成果解析摘要", y);
                y = drawText(contentStream, font, report.getParseSummary() != null ? report.getParseSummary() : "暂无", y);
                y -= LINE_SPACING;

                y = drawSectionTitle(contentStream, font, "三、评分详情", y);
                y = drawKeyValue(contentStream, font, "总分",
                        report.getTotalScore() != null ? report.getTotalScore().toString() + " 分" : "未评分", y);
                y -= LINE_SPACING;

                y = drawSectionTitle(contentStream, font, "四、教师评语", y);
                y = drawText(contentStream, font, report.getTeacherComment() != null ? report.getTeacherComment() : "暂无", y);
                y -= LINE_SPACING;

                if (report.getGenerateTime() != null) {
                    y = drawText(contentStream, font, "报告生成时间: " + report.getGenerateTime().format(DATE_FORMATTER), y);
                }
            }

            document.save(out);
            return out.toByteArray();
        }
    }

    private static float drawTitle(PDPageContentStream contentStream, PDFont font, String text, float y) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, FONT_SIZE_TITLE);
        contentStream.newLineAtOffset(MARGIN, y);
        contentStream.showText(text);
        contentStream.endText();
        return y - LINE_SPACING * 2;
    }

    private static float drawSectionTitle(PDPageContentStream contentStream, PDFont font, String text, float y) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, FONT_SIZE_SECTION);
        contentStream.newLineAtOffset(MARGIN, y);
        contentStream.showText(text);
        contentStream.endText();
        return y - LINE_SPACING;
    }

    private static float drawKeyValue(PDPageContentStream contentStream, PDFont font, String key, String value, float y) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, FONT_SIZE_NORMAL);
        contentStream.newLineAtOffset(MARGIN, y);
        contentStream.showText(key + ": " + value);
        contentStream.endText();
        return y - LINE_SPACING;
    }

    private static float drawText(PDPageContentStream contentStream, PDFont font, String text, float y) throws IOException {
        contentStream.beginText();
        contentStream.setFont(font, FONT_SIZE_NORMAL);
        contentStream.newLineAtOffset(MARGIN + 20, y);
        contentStream.showText(text);
        contentStream.endText();
        return y - LINE_SPACING;
    }

    public static byte[] generateClassReport(com.bisai.dto.ClassReportDTO report) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            PDPage page = new PDPage();
            document.addPage(page);

            PDFont font = loadChineseFont(document);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float y = page.getMediaBox().getHeight() - MARGIN;

                y = drawTitle(contentStream, font, "班级实训成果统计报告", y);
                y -= LINE_SPACING;

                y = drawSectionTitle(contentStream, font, "一、任务信息", y);
                y = drawKeyValue(contentStream, font, "实训任务", report.getTaskTitle() != null ? report.getTaskTitle() : "未知", y);
                y = drawKeyValue(contentStream, font, "所属课程", report.getCourseName() != null ? report.getCourseName() : "未知", y);
                y = drawKeyValue(contentStream, font, "学生总数", String.valueOf(report.getTotalStudents() != null ? report.getTotalStudents() : 0), y);
                y = drawKeyValue(contentStream, font, "已提交", String.valueOf(report.getSubmittedCount() != null ? report.getSubmittedCount() : 0), y);
                y = drawKeyValue(contentStream, font, "未提交", String.valueOf(report.getNotSubmittedCount() != null ? report.getNotSubmittedCount() : 0), y);
                y -= LINE_SPACING;

                y = drawSectionTitle(contentStream, font, "二、成绩统计", y);
                y = drawKeyValue(contentStream, font, "平均分", report.getAvgScore() != null ? report.getAvgScore().toString() + " 分" : "暂无", y);
                y = drawKeyValue(contentStream, font, "最高分", report.getMaxScore() != null ? report.getMaxScore().toString() + " 分" : "暂无", y);
                y = drawKeyValue(contentStream, font, "最低分", report.getMinScore() != null ? report.getMinScore().toString() + " 分" : "暂无", y);
                y -= LINE_SPACING;

                y = drawSectionTitle(contentStream, font, "三、分数分布", y);
                if (report.getScoreDistribution() != null) {
                    for (Map.Entry<String, Integer> entry : report.getScoreDistribution().entrySet()) {
                        y = drawKeyValue(contentStream, font, entry.getKey(), String.valueOf(entry.getValue()), y);
                    }
                }
                y -= LINE_SPACING;

                y = drawSectionTitle(contentStream, font, "四、优秀学生", y);
                if (report.getTopStudents() != null) {
                    for (com.bisai.dto.ClassReportDTO.StudentScoreSummary student : report.getTopStudents()) {
                        y = drawKeyValue(contentStream, font, student.getStudentName() != null ? student.getStudentName() : "未知",
                                student.getScore() != null ? student.getScore().toString() + " 分" : "暂无", y);
                    }
                }
                y -= LINE_SPACING;

                if (report.getGenerateTime() != null) {
                    drawText(contentStream, font, "报告生成时间: " + report.getGenerateTime(), y);
                }
            }

            document.save(out);
            return out.toByteArray();
        }
    }
}
