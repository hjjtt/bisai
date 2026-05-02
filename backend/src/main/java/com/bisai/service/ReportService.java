package com.bisai.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.annotation.ExcelProperty;
import com.bisai.common.Result;
import com.bisai.entity.*;
import com.bisai.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final SubmissionMapper submissionMapper;
    private final ScoreResultMapper scoreResultMapper;
    private final CheckResultMapper checkResultMapper;
    private final IndicatorMapper indicatorMapper;
    private final TrainingTaskMapper taskMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final FileMapper fileMapper;

    @Value("${file.upload-path}")
    private String uploadPath;

    /**
     * 导出学生个人报告
     */
    public Result<Map<String, Object>> exportStudentReport(Long submissionId, String format) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }

        try {
            // 获取关联数据
            TrainingTask task = taskMapper.selectById(submission.getTaskId());
            User student = userMapper.selectById(submission.getStudentId());
            Course course = task != null ? courseMapper.selectById(task.getCourseId()) : null;
            List<ScoreResult> scores = scoreResultMapper.selectList(
                    new LambdaQueryWrapper<ScoreResult>().eq(ScoreResult::getSubmissionId, submissionId)
            );
            List<CheckResult> checks = checkResultMapper.selectList(
                    new LambdaQueryWrapper<CheckResult>().eq(CheckResult::getSubmissionId, submissionId)
            );
            List<FileEntity> files = fileMapper.selectList(
                    new LambdaQueryWrapper<FileEntity>().eq(FileEntity::getSubmissionId, submissionId)
            );

            // 获取指标名称
            Map<Long, String> indicatorNameMap = new HashMap<>();
            if (!scores.isEmpty()) {
                List<Indicator> indicators = indicatorMapper.selectList(
                        new LambdaQueryWrapper<Indicator>()
                                .in(Indicator::getId, scores.stream().map(ScoreResult::getIndicatorId).collect(Collectors.toSet()))
                );
                indicatorNameMap = indicators.stream()
                        .collect(Collectors.toMap(Indicator::getId, Indicator::getName));
            }

            // 生成报告文件
            String fileName;
            if ("PDF".equalsIgnoreCase(format)) {
                fileName = generatePdfReport(submission, task, course, student, scores, checks, files, indicatorNameMap);
            } else {
                return Result.error(40001, "暂不支持" + format + "格式，请使用PDF格式");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("fileId", submissionId);
            data.put("fileName", fileName);
            data.put("downloadUrl", "/api/files/download/report/" + fileName);
            return Result.ok(data);

        } catch (Exception e) {
            log.error("生成学生报告失败: {}", e.getMessage(), e);
            return Result.error("报告生成失败: " + e.getMessage());
        }
    }

    /**
     * 导出班级统计报表
     */
    public Result<Map<String, Object>> exportClassReport(Long taskId, String format) {
        TrainingTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return Result.error(40401, "任务不存在");
        }

        try {
            // 获取该任务下所有已发布成绩的提交
            List<Submission> submissions = submissionMapper.selectList(
                    new LambdaQueryWrapper<Submission>()
                            .eq(Submission::getTaskId, taskId)
                            .eq(Submission::getScoreStatus, "PUBLISHED")
            );

            if (submissions.isEmpty()) {
                return Result.error("暂无已发布成绩的数据可导出");
            }

            String fileName;
            if ("EXCEL".equalsIgnoreCase(format)) {
                fileName = generateExcelReport(task, submissions);
            } else {
                return Result.error(40001, "暂不支持" + format + "格式，请使用Excel格式");
            }

            Map<String, Object> data = new HashMap<>();
            data.put("fileId", taskId);
            data.put("fileName", fileName);
            data.put("downloadUrl", "/api/files/download/report/" + fileName);
            return Result.ok(data);

        } catch (Exception e) {
            log.error("生成班级报表失败: {}", e.getMessage(), e);
            return Result.error("报表生成失败: " + e.getMessage());
        }
    }

    /**
     * 生成PDF格式的学生个人报告
     */
    private String generatePdfReport(Submission submission, TrainingTask task, Course course,
                                     User student, List<ScoreResult> scores, List<CheckResult> checks,
                                     List<FileEntity> files, Map<Long, String> indicatorNameMap) throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "学生报告_" + (student != null ? student.getRealName() : submission.getStudentId()) + "_" + timestamp + ".pdf";
        Path reportDir = Path.of(uploadPath, "reports");
        java.nio.file.Files.createDirectories(reportDir);
        Path filePath = reportDir.resolve(fileName);

        try (PdfWriter writer = new PdfWriter(filePath.toString());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            // 设置中文字体（使用内置字体）
            PdfFont font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            document.setFont(font);
            document.setFontSize(10);

            // 标题
            Paragraph title = new Paragraph("实训成果评价报告")
                    .setFont(font)
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // 基本信息表格
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 2})).useAllAvailableWidth();
            infoTable.addHeaderCell(createHeaderCell("学生姓名", font));
            infoTable.addCell(createCell(student != null ? student.getRealName() : "-", font));
            infoTable.addHeaderCell(createHeaderCell("学号", font));
            infoTable.addCell(createCell(student != null ? student.getUsername() : "-", font));
            infoTable.addHeaderCell(createHeaderCell("课程", font));
            infoTable.addCell(createCell(course != null ? course.getName() : "-", font));
            infoTable.addHeaderCell(createHeaderCell("任务", font));
            infoTable.addCell(createCell(task != null ? task.getTitle() : "-", font));
            infoTable.addHeaderCell(createHeaderCell("提交时间", font));
            infoTable.addCell(createCell(submission.getSubmitTime() != null ? submission.getSubmitTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-", font));
            infoTable.addHeaderCell(createHeaderCell("提交版本", font));
            infoTable.addCell(createCell("V" + submission.getVersion(), font));
            document.add(infoTable);
            document.add(new Paragraph("\n"));

            // 评分详情
            Paragraph scoreTitle = new Paragraph("评分详情")
                    .setFont(font)
                    .setFontSize(14)
                    .setBold();
            document.add(scoreTitle);
            document.add(new Paragraph("\n"));

            if (!scores.isEmpty()) {
                Table scoreTable = new Table(UnitValue.createPercentArray(new float[]{3, 1.5f, 1.5f, 1.5f, 4})).useAllAvailableWidth();
                scoreTable.addHeaderCell(createHeaderCell("评价指标", font));
                scoreTable.addHeaderCell(createHeaderCell("系统评分", font));
                scoreTable.addHeaderCell(createHeaderCell("教师评分", font));
                scoreTable.addHeaderCell(createHeaderCell("最终得分", font));
                scoreTable.addHeaderCell(createHeaderCell("评分理由", font));

                for (ScoreResult sr : scores) {
                    String indName = indicatorNameMap.getOrDefault(sr.getIndicatorId(), sr.getIndicatorName() != null ? sr.getIndicatorName() : "-");
                    scoreTable.addCell(createCell(indName, font));
                    scoreTable.addCell(createCell(sr.getAutoScore() != null ? sr.getAutoScore().toString() : "-", font));
                    scoreTable.addCell(createCell(sr.getTeacherScore() != null ? sr.getTeacherScore().toString() : "-", font));
                    scoreTable.addCell(createCell(sr.getFinalScore() != null ? sr.getFinalScore().toString() : "-", font));
                    scoreTable.addCell(createCell(sr.getReason() != null ? sr.getReason() : "-", font));
                }
                document.add(scoreTable);
            }

            // 总分
            document.add(new Paragraph("\n"));
            Paragraph totalScore = new Paragraph("总分：" + (submission.getTotalScore() != null ? submission.getTotalScore() : "未评分") + " 分")
                    .setFont(font)
                    .setFontSize(14)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(totalScore);
            document.add(new Paragraph("\n"));

            // 核查结果
            if (!checks.isEmpty()) {
                Paragraph checkTitle = new Paragraph("核查结果")
                        .setFont(font)
                        .setFontSize(14)
                        .setBold();
                document.add(checkTitle);
                document.add(new Paragraph("\n"));

                Table checkTable = new Table(UnitValue.createPercentArray(new float[]{2, 1.5f, 1.5f, 5})).useAllAvailableWidth();
                checkTable.addHeaderCell(createHeaderCell("核查项", font));
                checkTable.addHeaderCell(createHeaderCell("结果", font));
                checkTable.addHeaderCell(createHeaderCell("风险等级", font));
                checkTable.addHeaderCell(createHeaderCell("说明", font));

                for (CheckResult cr : checks) {
                    checkTable.addCell(createCell(cr.getCheckItem(), font));
                    checkTable.addCell(createCell(cr.getResult(), font));
                    checkTable.addCell(createCell(cr.getRiskLevel(), font));
                    checkTable.addCell(createCell(cr.getDescription() != null ? cr.getDescription() : "-", font));
                }
                document.add(checkTable);
            }

            // 教师评语
            if (submission.getTeacherComment() != null && !submission.getTeacherComment().isEmpty()) {
                document.add(new Paragraph("\n"));
                Paragraph commentTitle = new Paragraph("教师评语")
                        .setFont(font)
                        .setFontSize(14)
                        .setBold();
                document.add(commentTitle);
                document.add(new Paragraph(submission.getTeacherComment()).setFont(font));
            }

            // 生成时间
            document.add(new Paragraph("\n"));
            Paragraph generateTime = new Paragraph("报告生成时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .setFont(font)
                    .setFontSize(9)
                    .setTextAlignment(TextAlignment.RIGHT);
            document.add(generateTime);
        }

        return fileName;
    }

    /**
     * 生成Excel格式的班级统计报表
     */
    private String generateExcelReport(TrainingTask task, List<Submission> submissions) throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String fileName = "班级报表_" + task.getTitle() + "_" + timestamp + ".xlsx";
        Path reportDir = Path.of(uploadPath, "reports");
        java.nio.file.Files.createDirectories(reportDir);
        Path filePath = reportDir.resolve(fileName);

        // 收集所有评分指标
        List<Indicator> indicators = indicatorMapper.selectList(
                new LambdaQueryWrapper<Indicator>()
                        .eq(Indicator::getTemplateId, task.getTemplateId())
                        .isNull(Indicator::getParentId)
                        .orderByAsc(Indicator::getSortOrder)
        );

        // 构建Excel数据
        List<ClassReportRow> rows = new ArrayList<>();
        for (Submission sub : submissions) {
            ClassReportRow row = new ClassReportRow();
            User student = userMapper.selectById(sub.getStudentId());
            row.setStudentName(student != null ? student.getRealName() : "-");
            row.setStudentUsername(student != null ? student.getUsername() : "-");
            row.setTotalScore(sub.getTotalScore() != null ? sub.getTotalScore().doubleValue() : 0);

            // 获取该提交的各指标得分
            List<ScoreResult> scores = scoreResultMapper.selectList(
                    new LambdaQueryWrapper<ScoreResult>().eq(ScoreResult::getSubmissionId, sub.getId())
            );
            Map<Long, Double> scoreMap = scores.stream()
                    .collect(Collectors.toMap(ScoreResult::getIndicatorId,
                            sr -> sr.getFinalScore() != null ? sr.getFinalScore().doubleValue() : 0));

            // 动态设置指标得分
            List<Double> indicatorScores = new ArrayList<>();
            for (Indicator ind : indicators) {
                indicatorScores.add(scoreMap.getOrDefault(ind.getId(), 0.0));
            }
            row.setIndicatorScores(indicatorScores);

            rows.add(row);
        }

        // 写入Excel
        EasyExcel.write(filePath.toFile(), ClassReportRow.class)
                .sheet("班级统计")
                .doWrite(rows);

        return fileName;
    }

    /**
     * Excel数据行类
     */
    @Data
    public static class ClassReportRow {
        @ExcelProperty("学生姓名")
        private String studentName;

        @ExcelProperty("学号")
        private String studentUsername;

        @ExcelProperty("总分")
        private Double totalScore;

        // 动态指标列（通过includeColumnFiledNames控制）
        private List<Double> indicatorScores;
    }

    /**
     * 创建表头单元格
     */
    private com.itextpdf.layout.element.Cell createHeaderCell(String text, PdfFont font) {
        com.itextpdf.layout.element.Cell cell = new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(text).setFont(font).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
        return cell;
    }

    /**
     * 创建普通单元格
     */
    private com.itextpdf.layout.element.Cell createCell(String text, PdfFont font) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(text).setFont(font));
    }
}
