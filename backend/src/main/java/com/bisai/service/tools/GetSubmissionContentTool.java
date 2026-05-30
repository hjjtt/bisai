package com.bisai.service.tools;

import com.bisai.entity.CheckResult;
import com.bisai.entity.FileEntity;
import com.bisai.mapper.CheckResultMapper;
import com.bisai.mapper.FileMapper;
import com.bisai.service.DocumentTextExtractor;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Slf4j
@Component("getSubmissionContentTool")
@Description("根据 submissionId 获取学生提交的实训报告原文字符串。当你需要阅读学生作业原文时调用此工具。")
@RequiredArgsConstructor
public class GetSubmissionContentTool implements Function<GetSubmissionContentTool.Request, String> {

    private final FileMapper fileMapper;
    private final DocumentTextExtractor documentTextExtractor;
    private final CheckResultMapper checkResultMapper;
    private final ToolCallGuard toolCallGuard;

    public record Request(Long submissionId) {}

    @Override
    public String apply(Request request) {
        toolCallGuard.checkAndRecord(request.submissionId(), "getSubmissionContentTool");
        log.info("Agent 调用工具：GetSubmissionContentTool(submissionId={})", request.submissionId());
        try {
            // 1. 读取学生上传的作业内容
            List<FileEntity> files = fileMapper.selectList(
                    new LambdaQueryWrapper<FileEntity>().eq(FileEntity::getSubmissionId, request.submissionId())
            );
            if (files.isEmpty()) return "学生未提交任何文件";

            StringBuilder content = new StringBuilder();
            for (FileEntity file : files) {
                content.append("【文件名: ").append(file.getOriginalName()).append("】\n");
                String extracted = documentTextExtractor.extract(file).content();
                if (extracted != null && !extracted.isEmpty()) {
                    content.append(extracted).append("\n\n");
                }
            }

            // 2. 先截断正文，为核查结论腾出空间
            if (content.length() > 3500) {
                content = new StringBuilder(content.substring(0, 3500) + "...(正文过长已截断)\n");
            }

            // 3. 查询前置 CHECK 阶段的核查结论并拼接（确保核查结论始终完整保留）
            List<CheckResult> checks = checkResultMapper.selectList(
                    new LambdaQueryWrapper<CheckResult>()
                            .eq(CheckResult::getSubmissionId, request.submissionId())
            );

            if (checks != null && !checks.isEmpty()) {
                content.append("\n========================================\n");
                content.append("【前置核查结论】\n");
                content.append("以下为前置核查(CHECK)阶段发现的事实。评分时必须严格参考并引用以下结论：\n");
                for (CheckResult cr : checks) {
                    content.append(String.format("- [结果: %s] 类别: %s (%s) | 风险级别: %s\n  核查说明: %s\n  引用证据: %s\n",
                            cr.getResult(), cr.getCheckType(), cr.getCheckItem(), cr.getRiskLevel(),
                            cr.getDescription(), cr.getEvidence()));
                }
                content.append("========================================\n");
            }

            return content.toString();
        } catch (Exception e) {
            log.error("读取提交内容或核查结果失败", e);
            return "读取内容失败：" + e.getMessage();
        }
    }
}
