/*
 * Excel Report Generator
 * Author: echo
 * Date: 2026-05-01
 * Description: Excel报告生成工具类
 */

package com.bisai.util;

import com.bisai.dto.ClassReportDTO;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

@Slf4j
public class ExcelReportGenerator {

    public static byte[] generateClassReport(ClassReportDTO report) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle normalStyle = createNormalStyle(workbook);

            Sheet summarySheet = workbook.createSheet("班级概览");
            createClassSummarySheet(summarySheet, report, headerStyle, normalStyle);

            Sheet scoreSheet = workbook.createSheet("学生成绩");
            createStudentScoreSheet(scoreSheet, report, headerStyle, normalStyle);

            Sheet distributionSheet = workbook.createSheet("分数分布");
            createScoreDistributionSheet(distributionSheet, report, headerStyle, normalStyle);

            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                workbook.getSheetAt(i).autoSizeColumn(0);
                workbook.getSheetAt(i).autoSizeColumn(1);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private static void createClassSummarySheet(Sheet sheet, ClassReportDTO report, CellStyle headerStyle, CellStyle normalStyle) {
        int rowNum = 0;

        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("班级实训成果统计报告");
        titleCell.setCellStyle(headerStyle);

        rowNum++;

        Row taskRow = sheet.createRow(rowNum++);
        taskRow.createCell(0).setCellValue("实训任务");
        taskRow.createCell(1).setCellValue(report.getTaskTitle() != null ? report.getTaskTitle() : "");
        taskRow.getCell(0).setCellStyle(normalStyle);
        taskRow.getCell(1).setCellStyle(normalStyle);

        Row courseRow = sheet.createRow(rowNum++);
        courseRow.createCell(0).setCellValue("所属课程");
        courseRow.createCell(1).setCellValue(report.getCourseName() != null ? report.getCourseName() : "");
        courseRow.getCell(0).setCellStyle(normalStyle);
        courseRow.getCell(1).setCellStyle(normalStyle);

        rowNum++;

        Row statsRow1 = sheet.createRow(rowNum++);
        statsRow1.createCell(0).setCellValue("学生总数");
        statsRow1.createCell(1).setCellValue(report.getTotalStudents() != null ? report.getTotalStudents() : 0);
        statsRow1.createCell(2).setCellValue("已提交");
        statsRow1.createCell(3).setCellValue(report.getSubmittedCount() != null ? report.getSubmittedCount() : 0);
        statsRow1.createCell(4).setCellValue("未提交");
        statsRow1.createCell(5).setCellValue(report.getNotSubmittedCount() != null ? report.getNotSubmittedCount() : 0);
        for (int i = 0; i < 6; i++) {
            statsRow1.getCell(i).setCellStyle(normalStyle);
        }

        Row statsRow2 = sheet.createRow(rowNum++);
        statsRow2.createCell(0).setCellValue("平均分");
        statsRow2.createCell(1).setCellValue(report.getAvgScore() != null ? report.getAvgScore().doubleValue() : 0);
        statsRow2.createCell(2).setCellValue("最高分");
        statsRow2.createCell(3).setCellValue(report.getMaxScore() != null ? report.getMaxScore().doubleValue() : 0);
        statsRow2.createCell(4).setCellValue("最低分");
        statsRow2.createCell(5).setCellValue(report.getMinScore() != null ? report.getMinScore().doubleValue() : 0);
        for (int i = 0; i < 6; i++) {
            statsRow2.getCell(i).setCellStyle(normalStyle);
        }

        rowNum++;

        if (report.getCommonIssues() != null && !report.getCommonIssues().isEmpty()) {
            Row issueTitleRow = sheet.createRow(rowNum++);
            Cell issueTitleCell = issueTitleRow.createCell(0);
            issueTitleCell.setCellValue("常见问题");
            issueTitleCell.setCellStyle(headerStyle);

            for (String issue : report.getCommonIssues()) {
                Row issueRow = sheet.createRow(rowNum++);
                issueRow.createCell(0).setCellValue(issue);
                issueRow.getCell(0).setCellStyle(normalStyle);
            }
        }

        rowNum++;
        Row timeRow = sheet.createRow(rowNum);
        timeRow.createCell(0).setCellValue("报告生成时间");
        timeRow.createCell(1).setCellValue(report.getGenerateTime());
        timeRow.getCell(0).setCellStyle(normalStyle);
        timeRow.getCell(1).setCellStyle(normalStyle);
    }

    private static void createStudentScoreSheet(Sheet sheet, ClassReportDTO report, CellStyle headerStyle, CellStyle normalStyle) {
        int rowNum = 0;

        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"排名", "学生姓名", "成绩"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        if (report.getTopStudents() != null) {
            int rank = 1;
            for (ClassReportDTO.StudentScoreSummary student : report.getTopStudents()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(rank++);
                row.createCell(1).setCellValue(student.getStudentName() != null ? student.getStudentName() : "");
                row.createCell(2).setCellValue(student.getScore() != null ? student.getScore().doubleValue() : 0);
                for (int i = 0; i < 3; i++) {
                    row.getCell(i).setCellStyle(normalStyle);
                }
            }
        }
    }

    private static void createScoreDistributionSheet(Sheet sheet, ClassReportDTO report, CellStyle headerStyle, CellStyle normalStyle) {
        int rowNum = 0;

        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"分数段", "人数"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        if (report.getScoreDistribution() != null) {
            for (Map.Entry<String, Integer> entry : report.getScoreDistribution().entrySet()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(entry.getKey());
                row.createCell(1).setCellValue(entry.getValue());
                for (int i = 0; i < 2; i++) {
                    row.getCell(i).setCellStyle(normalStyle);
                }
            }
        }
    }

    private static CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);

        return style;
    }

    private static CellStyle createNormalStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);

        Font font = workbook.createFont();
        font.setFontName("宋体");
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        return style;
    }
}
