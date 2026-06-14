package com.bisai.controller;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.CorrectScoreRequest;
import com.bisai.dto.PageQuery;
import com.bisai.dto.ReturnRequest;
import com.bisai.dto.SaveScoresRequest;
import com.bisai.entity.FileEntity;
import com.bisai.entity.Submission;
import com.bisai.service.ScoreService;
import com.bisai.service.SubmissionService;
import jakarta.validation.Valid;
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
                                     Authentication auth) throws java.io.IOException {
        Long userId = (Long) auth.getPrincipal();
        // IOException 等异常交由全局处理器，避免吞掉事务回滚信息或泄漏内部错误
        return submissionService.uploadFiles(taskId, userId, files);
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

    // 学生查看自己的核查结果
    @GetMapping("/{id}/student-check-results")
    public Result<List<com.bisai.entity.CheckResult>> getStudentCheckResults(@PathVariable Long id, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return scoreService.getStudentCheckResults(id, userId);
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
                                           @Valid @RequestBody SaveScoresRequest request,
                                           Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        List<com.bisai.entity.ScoreResult> scores = request.getScores() != null
                ? request.getScores()
                : List.of();
        return scoreService.saveTeacherScores(id, scores, request.getComment(), request.getExpectedUpdatedAt(), userId, role);
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
    public Result<Void> returnSubmission(@PathVariable Long id, @Valid @RequestBody ReturnRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        return scoreService.returnSubmission(id, request.getReason(), userId, role);
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
    public Result<Void> correctScore(@PathVariable Long id, @Valid @RequestBody CorrectScoreRequest request, Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        String role = auth.getAuthorities().stream()
                .findFirst()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .orElse("");
        // reason 由 @NotBlank、newScore 由 @NotNull 在 @Valid 阶段校验
        return scoreService.correctScore(id, request.getIndicatorId(), request.getNewScore(), request.getReason(), userId, role);
    }
}
