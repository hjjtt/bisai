package com.bisai.controller;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.FileEntity;
import com.bisai.entity.Submission;
import com.bisai.service.ScoreService;
import com.bisai.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class SubmissionController {

    private final SubmissionService submissionService;
    private final ScoreService scoreService;

    @GetMapping
    public Result<PageResult<Submission>> list(PageQuery query,
                                                @RequestParam(required = false) Long taskId,
                                                @RequestParam(required = false) Long studentId,
                                                Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("STUDENT");
        return submissionService.listSubmissions(query, taskId, studentId, userId, role);
    }

    @GetMapping("/{id}")
    public Result<Submission> get(@PathVariable Long id) {
        return submissionService.getSubmission(id);
    }

    @PostMapping("/{taskId}/files")
    public Result<Void> uploadFiles(@PathVariable Long taskId,
                                     @RequestParam("files") MultipartFile[] files,
                                     Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        try {
            return submissionService.uploadFiles(taskId, userId, files);
        } catch (Exception e) {
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/{submissionId}/files")
    public Result<List<FileEntity>> getFileList(@PathVariable Long submissionId) {
        return submissionService.getFileList(submissionId);
    }

    // 智能解析 - 触发解析任务
    @PostMapping("/{id}/parse")
    public Result<Void> triggerParse(@PathVariable Long id) {
        return scoreService.triggerParse(id);
    }

    // 智能核查 - 触发核查任务
    @PostMapping("/{id}/check")
    public Result<Void> triggerCheck(@PathVariable Long id) {
        return scoreService.triggerCheck(id);
    }

    // 智能评分 - 触发评分任务
    @PostMapping("/{id}/score")
    public Result<Void> triggerScore(@PathVariable Long id) {
        return scoreService.triggerScore(id);
    }

    // 智能核查结果
    @GetMapping("/{id}/check-results")
    public Result<List<com.bisai.entity.CheckResult>> getCheckResults(@PathVariable Long id) {
        return scoreService.getCheckResults(id);
    }

    // 智能评分结果
    @GetMapping("/{id}/scores")
    public Result<List<com.bisai.entity.ScoreResult>> getScoreResults(@PathVariable Long id) {
        return scoreService.getScoreResults(id);
    }

    // 教师保存评分
    @PutMapping("/{id}/scores")
    public Result<Void> saveTeacherScores(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<com.bisai.entity.ScoreResult> scores =
                com.bisai.util.JsonUtil.convertList(body.get("scores"), com.bisai.entity.ScoreResult.class);
        String comment = (String) body.get("comment");
        return scoreService.saveTeacherScores(id, scores, comment);
    }

    // 发布成绩
    @PutMapping("/{id}/publish")
    public Result<Void> publishScore(@PathVariable Long id) {
        return scoreService.publishScore(id);
    }

    // 退回提交
    @PutMapping("/{id}/return")
    public Result<Void> returnSubmission(@PathVariable Long id, @RequestBody Map<String, String> body) {
        return scoreService.returnSubmission(id, body.get("reason"));
    }

    // 客观评分
    @GetMapping("/{id}/objective-score")
    public Result<Map<String, Object>> getObjectiveScore(@PathVariable Long id) {
        return scoreService.calculateObjectiveScore(id);
    }

    // 成绩修正
    @PutMapping("/{id}/correct")
    public Result<Void> correctScore(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        Long indicatorId = body.get("indicatorId") != null ? Long.valueOf(body.get("indicatorId").toString()) : null;
        java.math.BigDecimal newScore = new java.math.BigDecimal(body.get("newScore").toString());
        String reason = (String) body.get("reason");
        return scoreService.correctScore(id, indicatorId, newScore, reason, userId);
    }
}
