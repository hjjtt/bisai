package com.bisai.service;

import com.bisai.entity.*;
import com.bisai.mapper.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

/**
 * AI 智能服务 - 调用 ModelScope 实现解析、核查、评分
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final ModelScopeClient aiClient;

    private final SubmissionMapper submissionMapper;
    private final FileMapper fileMapper;
    private final CheckResultMapper checkResultMapper;
    private final ScoreResultMapper scoreResultMapper;
    private final TrainingTaskMapper taskMapper;
    private final IndicatorMapper indicatorMapper;

    // ==================== 智能解析 ====================

    /**
     * 智能解析提交文件内容
     */
    public void doParse(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) return;

        submission.setParseStatus("PARSING");
        submissionMapper.updateById(submission);

        try {
            // 获取提交文件列表
            List<FileEntity> files = fileMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileEntity>()
                            .eq(FileEntity::getSubmissionId, submissionId)
            );

            if (files.isEmpty()) {
                submission.setParseStatus("SUCCESS");
                submissionMapper.updateById(submission);
                return;
            }

            // 读取文件内容（文本类文件直接读取，非文本文件记录文件信息）
            StringBuilder fileContent = new StringBuilder();
            for (FileEntity file : files) {
                fileContent.append("【文件: ").append(file.getOriginalName())
                        .append(" | 类型: ").append(file.getFileType())
                        .append(" | 大小: ").append(file.getFileSize()).append("字节】\n");

                String content = readFileContent(file);
                if (content != null && !content.isEmpty()) {
                    // 截取前 8000 字符避免 token 过多
                    if (content.length() > 8000) {
                        content = content.substring(0, 8000) + "\n...(内容过长已截断)";
                    }
                    fileContent.append(content).append("\n\n");
                } else {
                    fileContent.append("(二进制文件，无法直接读取文本内容)\n\n");
                }
            }

            // 调用 AI 解析
            String systemPrompt = "你是一个文档解析助手。你需要分析学生提交的实训成果文件内容，提取关键信息。" +
                    "请以 JSON 格式返回解析结果，包含以下字段：\n" +
                    "- summary: 内容摘要（200字以内）\n" +
                    "- mainTopics: 主要涉及的知识点/主题（数组）\n" +
                    "- completeness: 完整度评估（HIGH/MEDIUM/LOW）\n" +
                    "- quality: 内容质量初步评估（HIGH/MEDIUM/LOW）\n" +
                    "- suggestions: 改进建议（数组）\n" +
                    "只返回 JSON，不要其他内容。";

            String aiResult = aiClient.chat(systemPrompt, fileContent.toString());
            log.info("解析结果(submissionId={}): {}", submissionId, aiResult.length() > 200 ? aiResult.substring(0, 200) + "..." : aiResult);

            submission.setParseStatus("SUCCESS");
            submissionMapper.updateById(submission);

        } catch (Exception e) {
            log.error("智能解析失败, submissionId={}: {}", submissionId, e.getMessage());
            submission.setParseStatus("FAILED");
            submissionMapper.updateById(submission);
        }
    }

    // ==================== 智能核查 ====================

    /**
     * 智能核查提交内容
     */
    public void doCheck(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) return;

        try {
            // 获取任务信息和要求
            TrainingTask task = taskMapper.selectById(submission.getTaskId());
            String taskRequirements = task != null ? task.getRequirements() : "";
            String taskTitle = task != null ? task.getTitle() : "";

            // 获取文件内容
            List<FileEntity> files = fileMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileEntity>()
                            .eq(FileEntity::getSubmissionId, submissionId)
            );

            StringBuilder fileContent = new StringBuilder();
            for (FileEntity file : files) {
                fileContent.append("【").append(file.getOriginalName()).append("】\n");
                String content = readFileContent(file);
                if (content != null) {
                    if (content.length() > 4000) content = content.substring(0, 4000) + "...";
                    fileContent.append(content).append("\n\n");
                }
            }

            // 构建核查 prompt
            String systemPrompt = "你是实训成果核查专家。你需要从以下维度核查学生提交的实训成果：\n" +
                    "1. **内容完整性** - 是否涵盖任务要求的所有要点\n" +
                    "2. **格式规范性** - 文档格式、代码风格是否规范\n" +
                    "3. **原创性评估** - 是否存在明显的抄袭痕迹（如格式混乱、内容不连贯等）\n" +
                    "4. **技术准确性** - 涉及的技术内容是否正确\n" +
                    "5. **任务匹配度** - 是否与任务要求相关\n\n" +
                    "请以 JSON 格式返回核查结果：\n" +
                    "{\n" +
                    "  \"items\": [\n" +
                    "    {\"checkType\": \"内容完整性\", \"checkItem\": \"检查项名称\", \"result\": \"PASS/WARNING/FAIL\", \"description\": \"详细说明\", \"evidence\": \"证据\", \"suggestion\": \"改进建议\", \"riskLevel\": \"LOW/MEDIUM/HIGH\"}\n" +
                    "  ]\n" +
                    "}\n" +
                    "只返回 JSON，不要其他内容。";

            String userMessage = "## 任务要求\n标题：" + taskTitle + "\n要求：" + taskRequirements + "\n\n## 学生提交内容\n" + fileContent;

            JsonNode result = aiClient.chatAsJson(systemPrompt, userMessage);

            // 清除旧的核查结果
            checkResultMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<CheckResult>()
                            .eq(CheckResult::getSubmissionId, submissionId)
            );

            // 保存核查结果
            JsonNode items = result.path("items");
            if (items.isArray()) {
                for (JsonNode item : items) {
                    CheckResult cr = new CheckResult();
                    cr.setSubmissionId(submissionId);
                    cr.setCheckType(item.path("checkType").asText("其他"));
                    cr.setCheckItem(item.path("checkItem").asText(""));
                    cr.setResult(item.path("result").asText("PASS"));
                    cr.setDescription(item.path("description").asText(""));
                    cr.setEvidence(item.path("evidence").asText(""));
                    cr.setSuggestion(item.path("suggestion").asText(""));
                    cr.setRiskLevel(item.path("riskLevel").asText("LOW"));
                    cr.setCreatedAt(LocalDateTime.now());
                    checkResultMapper.insert(cr);
                }
            }

            log.info("智能核查完成, submissionId={}, 检查项数={}", submissionId, items.size());

        } catch (Exception e) {
            log.error("智能核查失败, submissionId={}: {}", submissionId, e.getMessage());
            throw new RuntimeException("智能核查失败: " + e.getMessage());
        }
    }

    // ==================== 智能评分 ====================

    /**
     * 智能评分 - 基于评价指标自动打分
     */
    public void doScore(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) return;

        try {
            // 获取任务和评分模板
            TrainingTask task = taskMapper.selectById(submission.getTaskId());
            if (task == null || task.getTemplateId() == null) {
                log.warn("任务不存在或未关联评分模板, taskId={}", submission.getTaskId());
                submission.setScoreStatus("AI_SCORED");
                submissionMapper.updateById(submission);
                return;
            }

            // 获取评分指标
            List<Indicator> indicators = indicatorMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Indicator>()
                            .eq(Indicator::getTemplateId, task.getTemplateId())
                            .isNull(Indicator::getParentId)
                            .orderByAsc(Indicator::getSortOrder)
            );

            if (indicators.isEmpty()) {
                log.warn("评分模板没有指标, templateId={}", task.getTemplateId());
                submission.setScoreStatus("AI_SCORED");
                submissionMapper.updateById(submission);
                return;
            }

            // 获取文件内容
            List<FileEntity> files = fileMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<FileEntity>()
                            .eq(FileEntity::getSubmissionId, submissionId)
            );

            StringBuilder fileContent = new StringBuilder();
            for (FileEntity file : files) {
                fileContent.append("【").append(file.getOriginalName()).append("】\n");
                String content = readFileContent(file);
                if (content != null) {
                    if (content.length() > 3000) content = content.substring(0, 3000) + "...";
                    fileContent.append(content).append("\n\n");
                }
            }

            // 获取任务要求
            String requirements = task.getRequirements() != null ? task.getRequirements() : "";

            // 构建评分指标描述
            StringBuilder indicatorDesc = new StringBuilder();
            for (Indicator ind : indicators) {
                indicatorDesc.append("- ").append(ind.getName())
                        .append(" (满分: ").append(ind.getMaxScore()).append("分")
                        .append(", 权重: ").append(ind.getWeight()).append(")");
                if (ind.getScoreRule() != null && !ind.getScoreRule().isEmpty()) {
                    indicatorDesc.append(" 评分规则: ").append(ind.getScoreRule());
                }
                indicatorDesc.append("\n");

                // 获取子指标
                List<Indicator> children = indicatorMapper.selectList(
                        new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Indicator>()
                                .eq(Indicator::getParentId, ind.getId())
                                .orderByAsc(Indicator::getSortOrder)
                );
                for (Indicator child : children) {
                    indicatorDesc.append("  - ").append(child.getName())
                            .append(" (满分: ").append(child.getMaxScore()).append("分)\n");
                }
            }

            // 构建 AI 评分 prompt
            String systemPrompt = "你是实训成果评分专家。你需要根据给定的评分指标，对学生提交的实训成果进行客观评分。\n" +
                    "评分要求：\n" +
                    "1. 严格按照每个指标的满分范围打分\n" +
                    "2. 给出具体的评分理由\n" +
                    "3. 引用学生提交内容中的具体证据\n" +
                    "4. 评分要客观公正，不要过分宽松或严格\n\n" +
                    "请以 JSON 格式返回评分结果：\n" +
                    "{\n" +
                    "  \"scores\": [\n" +
                    "    {\"indicatorName\": \"指标名称\", \"score\": 分数, \"reason\": \"评分理由\", \"evidence\": \"证据引用\"}\n" +
                    "  ]\n" +
                    "}\n" +
                    "只返回 JSON，不要其他内容。";

            String userMessage = "## 任务要求\n" + requirements +
                    "\n\n## 评分指标\n" + indicatorDesc +
                    "\n## 学生提交内容\n" + fileContent;

            JsonNode result = aiClient.chatAsJson(systemPrompt, userMessage);

            // 清除旧的 AI 评分结果
            scoreResultMapper.delete(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<ScoreResult>()
                            .eq(ScoreResult::getSubmissionId, submissionId)
            );

            // 保存评分结果
            BigDecimal totalScore = BigDecimal.ZERO;
            JsonNode scores = result.path("scores");
            if (scores.isArray()) {
                for (JsonNode scoreItem : scores) {
                    String indName = scoreItem.path("indicatorName").asText("");

                    // 查找对应的指标
                    Indicator matchedIndicator = findIndicator(indicators, indName);

                    ScoreResult sr = new ScoreResult();
                    sr.setSubmissionId(submissionId);
                    sr.setIndicatorId(matchedIndicator != null ? matchedIndicator.getId() : null);
                    sr.setAutoScore(BigDecimal.valueOf(scoreItem.path("score").asDouble(0)));
                    sr.setReason(scoreItem.path("reason").asText(""));
                    sr.setEvidence(scoreItem.path("evidence").asText(""));
                    sr.setIndicatorName(indName);
                    sr.setMaxScore(matchedIndicator != null ? matchedIndicator.getMaxScore() : new BigDecimal("100"));
                    sr.setCreatedAt(LocalDateTime.now());
                    sr.setUpdatedAt(LocalDateTime.now());
                    scoreResultMapper.insert(sr);

                    if (sr.getAutoScore() != null) {
                        totalScore = totalScore.add(sr.getAutoScore());
                    }
                }
            }

            submission.setScoreStatus("AI_SCORED");
            submission.setTotalScore(totalScore);
            submissionMapper.updateById(submission);

            log.info("智能评分完成, submissionId={}, 评分项数={}, 总分={}", submissionId, scores.size(), totalScore);

        } catch (Exception e) {
            log.error("智能评分失败, submissionId={}: {}", submissionId, e.getMessage());
            submission.setScoreStatus("SCORE_FAILED");
            submissionMapper.updateById(submission);
            throw new RuntimeException("智能评分失败: " + e.getMessage());
        }
    }

    // ==================== 辅助方法 ====================

    /**
     * 读取文件文本内容
     */
    private String readFileContent(FileEntity file) {
        try {
            Path path = Path.of(file.getFilePath());
            if (!Files.exists(path)) return null;

            String fileType = file.getFileType();
            // 文本类文件直接读取
            if ("TXT".equalsIgnoreCase(fileType) || "MD".equalsIgnoreCase(fileType)
                    || "CSV".equalsIgnoreCase(fileType) || "JAVA".equalsIgnoreCase(fileType)
                    || "PY".equalsIgnoreCase(fileType) || "HTML".equalsIgnoreCase(fileType)
                    || "CSS".equalsIgnoreCase(fileType) || "JS".equalsIgnoreCase(fileType)
                    || "JSON".equalsIgnoreCase(fileType) || "XML".equalsIgnoreCase(fileType)
                    || "SQL".equalsIgnoreCase(fileType)) {
                return Files.readString(path);
            }

            // Word/Excel/PDF 等二进制文件，返回基础信息
            if ("DOC".equalsIgnoreCase(fileType) || "DOCX".equalsIgnoreCase(fileType)
                    || "PDF".equalsIgnoreCase(fileType) || "XLS".equalsIgnoreCase(fileType)
                    || "XLSX".equalsIgnoreCase(fileType)) {
                return "(该文件为" + fileType + "格式，内容需要专业解析工具读取。文件名: " + file.getOriginalName()
                        + ", 大小: " + formatFileSize(file.getFileSize()) + ")";
            }

            // 图片文件
            if ("JPG".equalsIgnoreCase(fileType) || "JPEG".equalsIgnoreCase(fileType)
                    || "PNG".equalsIgnoreCase(fileType)) {
                return "(图片文件: " + file.getOriginalName() + ", 大小: " + formatFileSize(file.getFileSize()) + ")";
            }

            // 压缩包
            if ("ZIP".equalsIgnoreCase(fileType) || "RAR".equalsIgnoreCase(fileType)) {
                return "(压缩文件: " + file.getOriginalName() + ", 大小: " + formatFileSize(file.getFileSize()) + ")";
            }

            return null;
        } catch (Exception e) {
            log.warn("读取文件内容失败: {}", file.getFilePath());
            return null;
        }
    }

    private String formatFileSize(Long bytes) {
        if (bytes == null) return "0B";
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        return String.format("%.1fMB", bytes / (1024.0 * 1024));
    }

    /**
     * 根据名称模糊匹配指标
     */
    private Indicator findIndicator(List<Indicator> indicators, String name) {
        if (name == null || name.isEmpty()) return null;
        for (Indicator ind : indicators) {
            if (name.contains(ind.getName()) || ind.getName().contains(name)) {
                return ind;
            }
        }
        // 如果没有匹配到，返回第一个（容错）
        return indicators.isEmpty() ? null : indicators.get(0);
    }
}
