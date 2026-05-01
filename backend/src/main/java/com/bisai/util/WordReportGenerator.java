/*
 * Word Report Generator
 * Author: echo
 * Date: 2026-05-01
 * Description: Word报告生成工具类
 */

package com.bisai.util;

import com.bisai.dto.StudentReportDTO;
import com.bisai.entity.CheckResult;
import com.bisai.entity.ScoreResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;

@Slf4j
public class WordReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    public static byte[] generateStudentReport(StudentReportDTO report) throws IOException {
        try (XWPFDocument document = new XWPFDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            addTitle(document, "实训成果评价报告");
            addEmptyLine(document);

            addSectionTitle(document, "一、基本信息");
            addKeyValueRow(document, "学生姓名", report.getStudentName());
            addKeyValueRow(document, "班级", report.getClassName() != null ? report.getClassName() : "未分配");
            addKeyValueRow(document, "实训任务", report.getTaskTitle());
            addKeyValueRow(document, "提交时间", report.getSubmitTime() != null ? report.getSubmitTime().format(DATE_FORMATTER) : "");
            addKeyValueRow(document, "版本号", String.valueOf(report.getVersion()));
            addEmptyLine(document);

            addSectionTitle(document, "二、成果解析摘要");
            addParagraph(document, report.getParseSummary() != null ? report.getParseSummary() : "暂无");
            addParagraph(document, "主题标签：" + (report.getParseTopics() != null ? report.getParseTopics() : "暂无"));
            addParagraph(document, "完整性评估：" + (report.getParseCompleteness() != null ? report.getParseCompleteness() : "暂无"));
            addParagraph(document, "质量评估：" + (report.getParseQuality() != null ? report.getParseQuality() : "暂无"));
            addParagraph(document, "改进建议：" + (report.getParseSuggestions() != null ? report.getParseSuggestions() : "暂无"));
            addEmptyLine(document);

            addSectionTitle(document, "三、智能核查结果");
            if (report.getCheckResults() != null && !report.getCheckResults().isEmpty()) {
                addCheckResultsTable(document, report.getCheckResults());
            } else {
                addParagraph(document, "暂无核查结果");
            }
            addEmptyLine(document);

            addSectionTitle(document, "四、评分详情");
            addKeyValueRow(document, "总分", report.getTotalScore() != null ? report.getTotalScore().toString() + " 分" : "未评分");
            if (report.getScoreResults() != null && !report.getScoreResults().isEmpty()) {
                addScoreResultsTable(document, report.getScoreResults());
            }
            addEmptyLine(document);

            addSectionTitle(document, "五、教师评语");
            addParagraph(document, report.getTeacherComment() != null ? report.getTeacherComment() : "暂无");
            addEmptyLine(document);

            addParagraph(document, "报告生成时间：" + report.getGenerateTime().format(DATE_FORMATTER));

            document.write(out);
            return out.toByteArray();
        }
    }

    private static void addTitle(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setAlignment(ParagraphAlignment.CENTER);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(18);
        run.setFontFamily("宋体");
    }

    private static void addSectionTitle(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(true);
        run.setFontSize(14);
        run.setFontFamily("宋体");
    }

    private static void addKeyValueRow(XWPFDocument document, String key, String value) {
        XWPFParagraph paragraph = document.createParagraph();
        XWPFRun keyRun = paragraph.createRun();
        keyRun.setText(key + "：");
        keyRun.setBold(true);
        keyRun.setFontSize(12);
        keyRun.setFontFamily("宋体");
        XWPFRun valueRun = paragraph.createRun();
        valueRun.setText(value);
        valueRun.setFontSize(12);
        valueRun.setFontFamily("宋体");
    }

    private static void addParagraph(XWPFDocument document, String text) {
        XWPFParagraph paragraph = document.createParagraph();
        paragraph.setIndentationFirstLine(400);
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setFontSize(12);
        run.setFontFamily("宋体");
    }

    private static void addEmptyLine(XWPFDocument document) {
        document.createParagraph();
    }

    private static void addCheckResultsTable(XWPFDocument document, java.util.List<CheckResult> results) {
        XWPFTable table = document.createTable(results.size() + 1, 5);
        table.setWidth("100%");

        XWPFTableRow headerRow = table.getRow(0);
        setCellText(headerRow.getCell(0), "核查类型", true);
        setCellText(headerRow.getCell(1), "核查项", true);
        setCellText(headerRow.getCell(2), "结果", true);
        setCellText(headerRow.getCell(3), "风险等级", true);
        setCellText(headerRow.getCell(4), "建议", true);

        for (int i = 0; i < results.size(); i++) {
            CheckResult result = results.get(i);
            XWPFTableRow row = table.getRow(i + 1);
            setCellText(row.getCell(0), result.getCheckType() != null ? result.getCheckType() : "");
            setCellText(row.getCell(1), result.getCheckItem() != null ? result.getCheckItem() : "");
            setCellText(row.getCell(2), result.getResult() != null ? result.getResult() : "");
            setCellText(row.getCell(3), result.getRiskLevel() != null ? result.getRiskLevel() : "");
            setCellText(row.getCell(4), result.getSuggestion() != null ? result.getSuggestion() : "");
        }
    }

    private static void addScoreResultsTable(XWPFDocument document, java.util.List<ScoreResult> results) {
        XWPFTable table = document.createTable(results.size() + 1, 4);
        table.setWidth("100%");

        XWPFTableRow headerRow = table.getRow(0);
        setCellText(headerRow.getCell(0), "指标名称", true);
        setCellText(headerRow.getCell(1), "AI评分", true);
        setCellText(headerRow.getCell(2), "教师评分", true);
        setCellText(headerRow.getCell(3), "最终得分", true);

        for (int i = 0; i < results.size(); i++) {
            ScoreResult result = results.get(i);
            XWPFTableRow row = table.getRow(i + 1);
            setCellText(row.getCell(0), result.getIndicatorName() != null ? result.getIndicatorName() : "");
            setCellText(row.getCell(1), result.getAutoScore() != null ? result.getAutoScore().toString() : "");
            setCellText(row.getCell(2), result.getTeacherScore() != null ? result.getTeacherScore().toString() : "");
            setCellText(row.getCell(3), result.getFinalScore() != null ? result.getFinalScore().toString() : "");
        }
    }

    private static void setCellText(XWPFTableCell cell, String text) {
        setCellText(cell, text, false);
    }

    private static void setCellText(XWPFTableCell cell, String text, boolean bold) {
        cell.removeParagraph(0);
        XWPFParagraph paragraph = cell.addParagraph();
        XWPFRun run = paragraph.createRun();
        run.setText(text);
        run.setBold(bold);
        run.setFontSize(11);
        run.setFontFamily("宋体");
    }
}
