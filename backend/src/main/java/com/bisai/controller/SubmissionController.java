package com.bisai.controller;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.FileEntity;
import com.bisai.entity.Submission;
import com.bisai.service.ScoreService;
import com.bisai.service.SubmissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    public Result<Submission> get(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("STUDENT");
        return submissionService.getSubmission(id, userId, role);
    }

    @PostMapping("/{taskId}/files")
    @PreAuthorize("hasRole('STUDENT')")
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
    public Result<List<FileEntity>> getFileList(@PathVariable Long submissionId, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("STUDENT");
        return submissionService.getFileList(submissionId, userId, role);
    }

    // 智能解析 - 触发解析任务
    @PostMapping("/{id}/parse")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> triggerParse(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        return scoreService.triggerParse(id, userId, role);
    }

    // 智能核查 - 触发核查任务
    @PostMapping("/{id}/check")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> triggerCheck(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        return scoreService.triggerCheck(id, userId, role);
    }

    // 智能评分 - 触发评分任务
    @PostMapping("/{id}/score")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> triggerScore(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        return scoreService.triggerScore(id, userId, role);
    }

    // 智能核查结果
    @GetMapping("/{id}/check-results")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<com.bisai.entity.CheckResult>> getCheckResults(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        return scoreService.getCheckResults(id, userId, role);
    }

    // 智能评分结果
    @GetMapping("/{id}/scores")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<List<com.bisai.entity.ScoreResult>> getScoreResults(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        return scoreService.getScoreResults(id, userId, role);
    }

    // 学生查看已发布成绩
    @GetMapping("/{id}/student-scores")
    public Result<List<com.bisai.entity.ScoreResult>> getStudentScores(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return scoreService.getStudentScores(id, userId);
    }

    // 教师保存评分
    @PutMapping("/{id}/scores")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> saveTeacherScores(@PathVariable Long id,
                                           @RequestBody Map<String, Object> body,
                                           Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        Object scoresObj = body.get("scores");
        List<com.bisai.entity.ScoreResult> scores = scoresObj != null
                ? com.bisai.util.JsonUtil.convertList(scoresObj, com.bisai.entity.ScoreResult.class)
                : List.of();
        String comment = (String) body.get("comment");
        String expectedUpdatedAt = (String) body.get("expectedUpdatedAt");
        return scoreService.saveTeacherScores(id, scores, comment, expectedUpdatedAt, userId, role);
    }

    // 发布成绩
    @PutMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> publishScore(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        return scoreService.publishScore(id, userId, role);
    }

    // 退回提交
    @PutMapping("/{id}/return")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> returnSubmission(@PathVariable Long id, @RequestBody Map<String, String> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        return scoreService.returnSubmission(id, body.get("reason"), userId, role);
    }

    // 客观评分
    @GetMapping("/{id}/objective-score")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Map<String, Object>> getObjectiveScore(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        return scoreService.calculateObjectiveScore(id, userId, role);
    }

    // 成绩修正
    @PutMapping("/{id}/correct")
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public Result<Void> correctScore(@PathVariable Long id, @RequestBody Map<String, Object> body, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        Object indicatorIdObj = body.get("indicatorId");
        Long indicatorId = indicatorIdObj != null ? Long.valueOf(indicatorIdObj.toString()) : null;
        Object newScoreObj = body.get("newScore");
        if (newScoreObj == null) return Result.error(40001, "newScore 不能为空");
        java.math.BigDecimal newScore = new java.math.BigDecimal(newScoreObj.toString());
        String reason = (String) body.get("reason");
        return scoreService.correctScore(id, indicatorId, newScore, reason, userId, role);
    }
}
