package com.bisai.service;

import com.alibaba.excel.EasyExcel;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartUtils;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;
import com.itextpdf.layout.element.Image;
import com.itextpdf.io.image.ImageDataFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.awt.Font;
import org.jfree.chart.StandardChartTheme;

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

    static {
        // 全局配置 JFreeChart 中文字体，解决乱码问题
        StandardChartTheme chartTheme = new StandardChartTheme("CN");
        // 设置标题字体
        chartTheme.setExtraLargeFont(new Font("Microsoft YaHei", Font.BOLD, 20));
        // 设置轴标签字体
        chartTheme.setLargeFont(new Font("Microsoft YaHei", Font.PLAIN, 15));
        // 设置常规字体
        chartTheme.setRegularFont(new Font("Microsoft YaHei", Font.PLAIN, 12));
        // 设置小字体
        chartTheme.setSmallFont(new Font("Microsoft YaHei", Font.PLAIN, 10));
        // 应用主题
        ChartFactory.setChartTheme(chartTheme);
    }

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
            } else if ("WORD".equalsIgnoreCase(format)) {
                fileName = generateWordReport(submission, task, course, student, scores, checks, files, indicatorNameMap);
            } else {
                return Result.error(40001, "暂不支持" + format + "格式，请使用PDF或WORD格式");
            }

            // 保存文件记录到 file 表
            Path reportPath = Path.of(uploadPath, "reports", fileName);
            FileEntity fileEntity = new FileEntity();
            fileEntity.setSubmissionId(submissionId);
            fileEntity.setOriginalName(fileName);
            fileEntity.setFilePath(reportPath.toString());
            fileEntity.setFileType("WORD".equalsIgnoreCase(format) ? "DOCX" : "PDF");
            fileEntity.setFileSize(java.nio.file.Files.size(reportPath));
            fileEntity.setFileHash(cn.hutool.crypto.digest.DigestUtil.md5Hex(reportPath.toFile()));
            fileMapper.insert(fileEntity);

            Map<String, Object> data = new HashMap<>();
            data.put("fileId", fileEntity.getId());
            data.put("fileName", fileName);
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
            } else if ("PDF".equalsIgnoreCase(format)) {
                fileName = generateClassPdfReport(task, submissions);
            } else {
                return Result.error(40001, "暂不支持" + format + "格式，请使用Excel或PDF格式");
            }

            // 保存文件记录到 file 表
            Path reportPath = Path.of(uploadPath, "reports", fileName);
            FileEntity fileEntity = new FileEntity();
            fileEntity.setSubmissionId(submissions.get(0).getId());
            fileEntity.setOriginalName(fileName);
            fileEntity.setFilePath(reportPath.toString());
            fileEntity.setFileType("PDF".equalsIgnoreCase(format) ? "PDF" : "XLSX");
            fileEntity.setFileSize(java.nio.file.Files.size(reportPath));
            fileEntity.setFileHash(cn.hutool.crypto.digest.DigestUtil.md5Hex(reportPath.toFile()));
            fileMapper.insert(fileEntity);

            Map<String, Object> data = new HashMap<>();
            data.put("fileId", fileEntity.getId());
            data.put("fileName", fileName);
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
        String studentLabel = student != null && student.getRealName() != null ? student.getRealName() : String.valueOf(submission.getStudentId());
        String fileName = "学生报告_" + studentLabel + "_" + timestamp + ".pdf";
        // 过滤文件名中的特殊字符
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
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
            infoTable.addCell(createHeaderCell("学生姓名", font));
            infoTable.addCell(createCell(student != null ? student.getRealName() : "-", font));
            infoTable.addCell(createHeaderCell("学号", font));
            infoTable.addCell(createCell(student != null ? student.getUsername() : "-", font));
            infoTable.addCell(createHeaderCell("课程", font));
            infoTable.addCell(createCell(course != null ? course.getName() : "-", font));
            infoTable.addCell(createHeaderCell("任务", font));
            infoTable.addCell(createCell(task != null ? task.getTitle() : "-", font));
            infoTable.addCell(createHeaderCell("提交时间", font));
            infoTable.addCell(createCell(submission.getSubmitTime() != null ? submission.getSubmitTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-", font));
            infoTable.addCell(createHeaderCell("提交版本", font));
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
                // 可视化图表 - 个人指标得分图
                try {
                    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                    for (ScoreResult sr : scores) {
                        String indName = indicatorNameMap.getOrDefault(sr.getIndicatorId(), "指标");
                        double finalScore = sr.getFinalScore() != null ? sr.getFinalScore().doubleValue() : 0;
                        dataset.addValue(finalScore, "得分", indName);
                    }
                    JFreeChart chart = ChartFactory.createBarChart("得分对比图", "评价指标", "分数", dataset, PlotOrientation.VERTICAL, false, true, false);
                    ByteArrayOutputStream chartOut = new ByteArrayOutputStream();
                    ChartUtils.writeChartAsPNG(chartOut, chart, 500, 300);
                    Image chartImg = new Image(ImageDataFactory.create(chartOut.toByteArray())).setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                    document.add(chartImg);
                    document.add(new Paragraph("\n"));
                } catch (Exception e) {
                    log.warn("个人报告图表生成失败: {}", e.getMessage());
                }

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
     * 生成Word格式的学生个人报告
     */
    private String generateWordReport(Submission submission, TrainingTask task, Course course,
                                      User student, List<ScoreResult> scores, List<CheckResult> checks,
                                      List<FileEntity> files, Map<Long, String> indicatorNameMap) throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String studentLabel = student != null && student.getRealName() != null ? student.getRealName() : String.valueOf(submission.getStudentId());
        String fileName = "学生报告_" + studentLabel + "_" + timestamp + ".docx";
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        Path reportDir = Path.of(uploadPath, "reports");
        java.nio.file.Files.createDirectories(reportDir);
        Path filePath = reportDir.resolve(fileName);

        try (XWPFDocument document = new XWPFDocument()) {
            // 标题
            XWPFParagraph titleParagraph = document.createParagraph();
            titleParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titleParagraph.createRun();
            titleRun.setText("实训成果评价报告");
            titleRun.setBold(true);
            titleRun.setFontSize(18);

            // 空行
            document.createParagraph();

            // 基本信息表格
            XWPFTable infoTable = document.createTable(6, 2);
            infoTable.setWidth("100%");

            addTableRow(infoTable, 0, "学生姓名", student != null ? student.getRealName() : "-");
            addTableRow(infoTable, 1, "学号", student != null ? student.getUsername() : "-");
            addTableRow(infoTable, 2, "课程", course != null ? course.getName() : "-");
            addTableRow(infoTable, 3, "任务", task != null ? task.getTitle() : "-");
            addTableRow(infoTable, 4, "提交时间", submission.getSubmitTime() != null ?
                    submission.getSubmitTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "-");
            addTableRow(infoTable, 5, "提交版本", "V" + submission.getVersion());

            // 空行
            document.createParagraph();

            // 评分详情标题
            XWPFParagraph scoreTitleParagraph = document.createParagraph();
            XWPFRun scoreTitleRun = scoreTitleParagraph.createRun();
            scoreTitleRun.setText("评分详情");
            scoreTitleRun.setBold(true);
            scoreTitleRun.setFontSize(14);

            // 可视化图表 - 个人指标得分图
            if (!scores.isEmpty()) {
                try {
                    DefaultCategoryDataset dataset = new DefaultCategoryDataset();
                    for (ScoreResult sr : scores) {
                        String indName = indicatorNameMap.getOrDefault(sr.getIndicatorId(), "指标");
                        double finalScore = sr.getFinalScore() != null ? sr.getFinalScore().doubleValue() : 0;
                        dataset.addValue(finalScore, "得分", indName);
                    }
                    JFreeChart chart = ChartFactory.createBarChart("得分对比图", "评价指标", "分数", dataset, PlotOrientation.VERTICAL, false, true, false);
                    ByteArrayOutputStream chartOut = new ByteArrayOutputStream();
                    ChartUtils.writeChartAsPNG(chartOut, chart, 500, 300);
                    
                    XWPFParagraph imgPara = document.createParagraph();
                    imgPara.setAlignment(ParagraphAlignment.CENTER);
                    XWPFRun imgRun = imgPara.createRun();
                    try (java.io.ByteArrayInputStream bis = new java.io.ByteArrayInputStream(chartOut.toByteArray())) {
                        imgRun.addPicture(bis, XWPFDocument.PICTURE_TYPE_PNG, "chart.png", 
                                org.apache.poi.util.Units.toEMU(400), org.apache.poi.util.Units.toEMU(240));
                    }
                } catch (Exception e) {
                    log.warn("Word报告图表生成失败: {}", e.getMessage());
                }
            }

            // 空行
            document.createParagraph();

            // 评分详情表格
            if (!scores.isEmpty()) {
                XWPFTable scoreTable = document.createTable(scores.size() + 1, 5);
                scoreTable.setWidth("100%");

                // 表头
                addTableHeaderCell(scoreTable, 0, 0, "评价指标");
                addTableHeaderCell(scoreTable, 0, 1, "系统评分");
                addTableHeaderCell(scoreTable, 0, 2, "教师评分");
                addTableHeaderCell(scoreTable, 0, 3, "最终得分");
                addTableHeaderCell(scoreTable, 0, 4, "评分理由");

                // 数据行
                for (int i = 0; i < scores.size(); i++) {
                    ScoreResult sr = scores.get(i);
                    String indName = indicatorNameMap.getOrDefault(sr.getIndicatorId(),
                            sr.getIndicatorName() != null ? sr.getIndicatorName() : "-");
                    addTableCell(scoreTable, i + 1, 0, indName);
                    addTableCell(scoreTable, i + 1, 1, sr.getAutoScore() != null ? sr.getAutoScore().toString() : "-");
                    addTableCell(scoreTable, i + 1, 2, sr.getTeacherScore() != null ? sr.getTeacherScore().toString() : "-");
                    addTableCell(scoreTable, i + 1, 3, sr.getFinalScore() != null ? sr.getFinalScore().toString() : "-");
                    addTableCell(scoreTable, i + 1, 4, sr.getReason() != null ? sr.getReason() : "-");
                }
            }

            // 空行
            document.createParagraph();

            // 总分
            XWPFParagraph totalScoreParagraph = document.createParagraph();
            totalScoreParagraph.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun totalScoreRun = totalScoreParagraph.createRun();
            totalScoreRun.setText("总分：" + (submission.getTotalScore() != null ? submission.getTotalScore() : "未评分") + " 分");
            totalScoreRun.setBold(true);
            totalScoreRun.setFontSize(14);

            // 空行
            document.createParagraph();

            // 核查结果
            if (!checks.isEmpty()) {
                XWPFParagraph checkTitleParagraph = document.createParagraph();
                XWPFRun checkTitleRun = checkTitleParagraph.createRun();
                checkTitleRun.setText("核查结果");
                checkTitleRun.setBold(true);
                checkTitleRun.setFontSize(14);

                // 空行
                document.createParagraph();

                XWPFTable checkTable = document.createTable(checks.size() + 1, 4);
                checkTable.setWidth("100%");

                // 表头
                addTableHeaderCell(checkTable, 0, 0, "核查项");
                addTableHeaderCell(checkTable, 0, 1, "结果");
                addTableHeaderCell(checkTable, 0, 2, "风险等级");
                addTableHeaderCell(checkTable, 0, 3, "说明");

                // 数据行
                for (int i = 0; i < checks.size(); i++) {
                    CheckResult cr = checks.get(i);
                    addTableCell(checkTable, i + 1, 0, cr.getCheckItem());
                    addTableCell(checkTable, i + 1, 1, cr.getResult());
                    addTableCell(checkTable, i + 1, 2, cr.getRiskLevel());
                    addTableCell(checkTable, i + 1, 3, cr.getDescription() != null ? cr.getDescription() : "-");
                }
            }

            // 教师评语
            if (submission.getTeacherComment() != null && !submission.getTeacherComment().isEmpty()) {
                document.createParagraph();
                XWPFParagraph commentTitleParagraph = document.createParagraph();
                XWPFRun commentTitleRun = commentTitleParagraph.createRun();
                commentTitleRun.setText("教师评语");
                commentTitleRun.setBold(true);
                commentTitleRun.setFontSize(14);

                document.createParagraph();
                XWPFParagraph commentParagraph = document.createParagraph();
                XWPFRun commentRun = commentParagraph.createRun();
                commentRun.setText(submission.getTeacherComment());
            }

            // 生成时间
            document.createParagraph();
            XWPFParagraph footerParagraph = document.createParagraph();
            footerParagraph.setAlignment(ParagraphAlignment.RIGHT);
            XWPFRun footerRun = footerParagraph.createRun();
            footerRun.setText("报告生成时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            footerRun.setFontSize(9);

            // 保存文件
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(filePath.toFile())) {
                document.write(fos);
            }
        }

        return fileName;
    }

    /**
     * 添加Word表格行
     */
    private void addTableRow(XWPFTable table, int rowIndex, String label, String value) {
        XWPFTableRow row = table.getRow(rowIndex);
        row.getCell(0).setText(label);
        row.getCell(1).setText(value);
    }

    /**
     * 添加Word表格表头单元格
     */
    private void addTableHeaderCell(XWPFTable table, int rowIndex, int colIndex, String text) {
        XWPFTableCell cell = table.getRow(rowIndex).getCell(colIndex);
        cell.setText(text);
        // 设置表头样式
        XWPFParagraph paragraph = cell.getParagraphs().get(0);
        XWPFRun run = paragraph.createRun();
        run.setBold(true);
    }

    /**
     * 添加Word表格普通单元格
     */
    private void addTableCell(XWPFTable table, int rowIndex, int colIndex, String text) {
        XWPFTableCell cell = table.getRow(rowIndex).getCell(colIndex);
        cell.setText(text);
    }

    /**
     * 生成Excel格式的班级统计报表
     */
    private String generateExcelReport(TrainingTask task, List<Submission> submissions) throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String taskLabel = task.getTitle() != null ? task.getTitle() : "task-" + task.getId();
        String fileName = "班级报表_" + taskLabel + "_" + timestamp + ".xlsx";
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
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

        // 构建Excel数据 - 批量查询避免N+1
        Set<Long> studentIds = submissions.stream().map(Submission::getStudentId).collect(Collectors.toSet());
        Set<Long> subIds = submissions.stream().map(Submission::getId).collect(Collectors.toSet());

        Map<Long, User> studentMap = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, studentIds)).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<ScoreResult> allScores = scoreResultMapper.selectList(
                new LambdaQueryWrapper<ScoreResult>().in(ScoreResult::getSubmissionId, subIds)
        );
        Map<Long, Map<Long, Double>> scoreBySubmission = allScores.stream()
                .collect(Collectors.groupingBy(ScoreResult::getSubmissionId,
                        Collectors.toMap(ScoreResult::getIndicatorId,
                                sr -> sr.getFinalScore() != null ? sr.getFinalScore().doubleValue() : 0)));

        // 构建动态表头
        List<List<String>> head = new ArrayList<>();
        head.add(Collections.singletonList("学生姓名"));
        head.add(Collections.singletonList("学号"));
        for (Indicator ind : indicators) {
            head.add(Collections.singletonList(ind.getName()));
        }
        head.add(Collections.singletonList("总分"));

        // 构建数据行
        List<List<Object>> data = new ArrayList<>();
        for (Submission sub : submissions) {
            List<Object> row = new ArrayList<>();
            User student = studentMap.get(sub.getStudentId());
            row.add(student != null ? student.getRealName() : "-");
            row.add(student != null ? student.getUsername() : "-");

            Map<Long, Double> scoreMap = scoreBySubmission.getOrDefault(sub.getId(), Map.of());
            for (Indicator ind : indicators) {
                row.add(scoreMap.getOrDefault(ind.getId(), 0.0));
            }
            row.add(sub.getTotalScore() != null ? sub.getTotalScore().doubleValue() : 0);
            data.add(row);
        }

        // 写入Excel（使用动态表头）
        EasyExcel.write(filePath.toFile())
                .head(head)
                .sheet("班级统计")
                .doWrite(data);

        return fileName;
    }

    /**
     * 生成PDF格式的班级统计报表
     */
    private String generateClassPdfReport(TrainingTask task, List<Submission> submissions) throws Exception {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String taskLabel = task.getTitle() != null ? task.getTitle() : "task-" + task.getId();
        String fileName = "班级报表_" + taskLabel + "_" + timestamp + ".pdf";
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
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

        // 构建数据 - 批量查询避免N+1
        Set<Long> studentIds = submissions.stream().map(Submission::getStudentId).collect(Collectors.toSet());
        Set<Long> subIds = submissions.stream().map(Submission::getId).collect(Collectors.toSet());

        Map<Long, User> studentMap = userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getId, studentIds)).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        List<ScoreResult> allScores = scoreResultMapper.selectList(
                new LambdaQueryWrapper<ScoreResult>().in(ScoreResult::getSubmissionId, subIds)
        );
        Map<Long, Map<Long, Double>> scoreBySubmission = allScores.stream()
                .collect(Collectors.groupingBy(ScoreResult::getSubmissionId,
                        Collectors.toMap(ScoreResult::getIndicatorId,
                                sr -> sr.getFinalScore() != null ? sr.getFinalScore().doubleValue() : 0)));

        // 获取课程信息
        Course course = courseMapper.selectById(task.getCourseId());

        // 生成PDF
        try (PdfWriter writer = new PdfWriter(filePath.toString());
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {

            PdfFont font = PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H", PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            document.setFont(font);
            document.setFontSize(10);

            // 标题
            Paragraph title = new Paragraph("班级统计报表")
                    .setFont(font)
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            // 基本信息
            Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 1, 2})).useAllAvailableWidth();
            infoTable.addCell(createHeaderCell("课程名称", font));
            infoTable.addCell(createCell(course != null ? course.getName() : "-", font));
            infoTable.addCell(createHeaderCell("实训任务", font));
            infoTable.addCell(createCell(task.getTitle(), font));
            infoTable.addCell(createHeaderCell("学生人数", font));
            infoTable.addCell(createCell(String.valueOf(submissions.size()), font));
            infoTable.addCell(createHeaderCell("生成时间", font));
            infoTable.addCell(createCell(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")), font));
            document.add(infoTable);
            document.add(new Paragraph("\n"));

            // 统计摘要
            List<Submission> scoredSubmissions = submissions.stream()
                    .filter(s -> s.getTotalScore() != null)
                    .collect(Collectors.toList());

            if (!scoredSubmissions.isEmpty()) {
                double avg = scoredSubmissions.stream().mapToDouble(s -> s.getTotalScore().doubleValue()).average().orElse(0);
                double max = scoredSubmissions.stream().mapToDouble(s -> s.getTotalScore().doubleValue()).max().orElse(0);
                double min = scoredSubmissions.stream().mapToDouble(s -> s.getTotalScore().doubleValue()).min().orElse(0);
                long passCount = scoredSubmissions.stream()
                        .filter(s -> s.getTotalScore().compareTo(BigDecimal.valueOf(60)) >= 0)
                        .count();

                Paragraph summaryTitle = new Paragraph("成绩统计")
                        .setFont(font)
                        .setFontSize(14)
                        .setBold();
                document.add(summaryTitle);
                document.add(new Paragraph("\n"));

                Table summaryTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1})).useAllAvailableWidth();
                summaryTable.addCell(createHeaderCell("平均分", font));
                summaryTable.addCell(createCell(String.format("%.2f", avg), font));
                summaryTable.addCell(createHeaderCell("最高分", font));
                summaryTable.addCell(createCell(String.format("%.2f", max), font));
                summaryTable.addCell(createHeaderCell("最低分", font));
                summaryTable.addCell(createCell(String.format("%.2f", min), font));
                summaryTable.addCell(createHeaderCell("及格率", font));
                summaryTable.addCell(createCell(String.format("%.1f%%", passCount * 100.0 / scoredSubmissions.size()), font));
                document.add(summaryTable);
                document.add(new Paragraph("\n"));

                // 可视化图表 - 班级成绩分布图
                try {
                    DefaultPieDataset pieDataset = new DefaultPieDataset();
                    long excellent = scoredSubmissions.stream().filter(s -> s.getTotalScore().compareTo(BigDecimal.valueOf(90)) >= 0).count();
                    long good = scoredSubmissions.stream().filter(s -> s.getTotalScore().compareTo(BigDecimal.valueOf(80)) >= 0 && s.getTotalScore().compareTo(BigDecimal.valueOf(90)) < 0).count();
                    long pass = scoredSubmissions.stream().filter(s -> s.getTotalScore().compareTo(BigDecimal.valueOf(60)) >= 0 && s.getTotalScore().compareTo(BigDecimal.valueOf(80)) < 0).count();
                    long fail = scoredSubmissions.stream().filter(s -> s.getTotalScore().compareTo(BigDecimal.valueOf(60)) < 0).count();
                    
                    if (excellent > 0) pieDataset.setValue("优秀(90+)", excellent);
                    if (good > 0) pieDataset.setValue("良好(80-89)", good);
                    if (pass > 0) pieDataset.setValue("及格(60-79)", pass);
                    if (fail > 0) pieDataset.setValue("不及格(<60)", fail);

                    JFreeChart pieChart = ChartFactory.createPieChart("成绩等级分布", pieDataset, true, true, false);
                    ByteArrayOutputStream pieOut = new ByteArrayOutputStream();
                    ChartUtils.writeChartAsPNG(pieOut, pieChart, 400, 300);
                    Image pieImg = new Image(ImageDataFactory.create(pieOut.toByteArray())).setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                    document.add(pieImg);
                } catch (Exception e) {
                    log.warn("班级分布图生成失败: {}", e.getMessage());
                }

                // 可视化图表 - 指标达成度柱状图
                try {
                    DefaultCategoryDataset barDataset = new DefaultCategoryDataset();
                    for (Indicator ind : indicators) {
                        double indAvg = allScores.stream()
                                .filter(s -> s.getIndicatorId().equals(ind.getId()))
                                .mapToDouble(s -> s.getFinalScore() != null ? s.getFinalScore().doubleValue() : 0)
                                .average().orElse(0);
                        barDataset.addValue(indAvg, "平均分", ind.getName());
                    }
                    JFreeChart barChart = ChartFactory.createBarChart("各指标平均达成度", "指标名称", "平均分数", barDataset, PlotOrientation.VERTICAL, false, true, false);
                    ByteArrayOutputStream barOut = new ByteArrayOutputStream();
                    ChartUtils.writeChartAsPNG(barOut, barChart, 500, 300);
                    Image barImg = new Image(ImageDataFactory.create(barOut.toByteArray())).setHorizontalAlignment(com.itextpdf.layout.properties.HorizontalAlignment.CENTER);
                    document.add(barImg);
                    document.add(new Paragraph("\n"));
                } catch (Exception e) {
                    log.warn("指标达成度图生成失败: {}", e.getMessage());
                }
            }

            // 学生成绩明细表
            Paragraph detailTitle = new Paragraph("学生成绩明细")
                    .setFont(font)
                    .setFontSize(14)
                    .setBold();
            document.add(detailTitle);
            document.add(new Paragraph("\n"));

            // 动态列：学号、姓名、各指标、总分
            int colCount = 3 + indicators.size();
            float[] colWidths = new float[colCount];
            colWidths[0] = 1.5f; // 学号
            colWidths[1] = 1.5f; // 姓名
            for (int i = 0; i < indicators.size(); i++) {
                colWidths[2 + i] = 1.5f; // 各指标
            }
            colWidths[colCount - 1] = 1.5f; // 总分

            Table detailTable = new Table(UnitValue.createPercentArray(colWidths)).useAllAvailableWidth();
            detailTable.addHeaderCell(createHeaderCell("学号", font));
            detailTable.addHeaderCell(createHeaderCell("姓名", font));
            for (Indicator ind : indicators) {
                detailTable.addHeaderCell(createHeaderCell(ind.getName(), font));
            }
            detailTable.addHeaderCell(createHeaderCell("总分", font));

            for (Submission sub : submissions) {
                User student = studentMap.get(sub.getStudentId());
                detailTable.addCell(createCell(student != null ? student.getUsername() : "-", font));
                detailTable.addCell(createCell(student != null ? student.getRealName() : "-", font));

                Map<Long, Double> scoreMap = scoreBySubmission.getOrDefault(sub.getId(), Map.of());
                for (Indicator ind : indicators) {
                    Double score = scoreMap.getOrDefault(ind.getId(), 0.0);
                    detailTable.addCell(createCell(String.format("%.1f", score), font));
                }
                detailTable.addCell(createCell(sub.getTotalScore() != null ? String.format("%.1f", sub.getTotalScore()) : "-", font));
            }
            document.add(detailTable);

            // 页脚
            document.add(new Paragraph("\n"));
            Paragraph footer = new Paragraph("报表生成时间：" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .setFont(font)
                    .setFontSize(9)
                    .setTextAlignment(TextAlignment.RIGHT);
            document.add(footer);
        }

        return fileName;
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
