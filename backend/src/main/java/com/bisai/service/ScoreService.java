package com.bisai.service;

import com.bisai.common.Result;
import com.bisai.entity.CheckResult;
import com.bisai.entity.ScoreResult;
import com.bisai.entity.Submission;
import com.bisai.mapper.CheckResultMapper;
import com.bisai.mapper.ScoreResultMapper;
import com.bisai.mapper.SubmissionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScoreService {

    private final SubmissionMapper submissionMapper;
    private final ScoreResultMapper scoreResultMapper;
    private final CheckResultMapper checkResultMapper;
    private final AiService aiService;

    /**
     * 触发智能解析
     */
    public Result<Void> triggerParse(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }
        // 异步执行解析（当前为同步，后续可用线程池优化）
        try {
            aiService.doParse(submissionId);
            return Result.ok();
        } catch (Exception e) {
            log.error("触发解析失败: {}", e.getMessage());
            return Result.error("解析失败: " + e.getMessage());
        }
    }

    /**
     * 触发智能核查
     */
    public Result<Void> triggerCheck(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }
        try {
            aiService.doCheck(submissionId);
            return Result.ok();
        } catch (Exception e) {
            log.error("触发核查失败: {}", e.getMessage());
            return Result.error("核查失败: " + e.getMessage());
        }
    }

    /**
     * 触发智能评分
     */
    public Result<Void> triggerScore(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }
        try {
            aiService.doScore(submissionId);
            return Result.ok();
        } catch (Exception e) {
            log.error("触发评分失败: {}", e.getMessage());
            return Result.error("评分失败: " + e.getMessage());
        }
    }

    public Result<List<CheckResult>> getCheckResults(Long submissionId) {
        List<CheckResult> results = checkResultMapper.selectList(
                new LambdaQueryWrapper<CheckResult>().eq(CheckResult::getSubmissionId, submissionId)
        );
        return Result.ok(results);
    }

    public Result<List<ScoreResult>> getScoreResults(Long submissionId) {
        List<ScoreResult> results = scoreResultMapper.selectList(
                new LambdaQueryWrapper<ScoreResult>().eq(ScoreResult::getSubmissionId, submissionId)
        );
        return Result.ok(results);
    }

    @Transactional
    public Result<Void> saveTeacherScores(Long submissionId, List<ScoreResult> scores, String comment) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }

        BigDecimal totalScore = BigDecimal.ZERO;
        for (ScoreResult score : scores) {
            score.setSubmissionId(submissionId);
            score.setFinalScore(score.getTeacherScore());

            ScoreResult existing = scoreResultMapper.selectOne(
                    new LambdaQueryWrapper<ScoreResult>()
                            .eq(ScoreResult::getSubmissionId, submissionId)
                            .eq(ScoreResult::getIndicatorId, score.getIndicatorId())
            );
            if (existing != null) {
                score.setId(existing.getId());
                scoreResultMapper.updateById(score);
            } else {
                scoreResultMapper.insert(score);
            }

            if (score.getTeacherScore() != null) {
                totalScore = totalScore.add(score.getTeacherScore());
            }
        }

        submission.setTeacherComment(comment);
        submission.setTotalScore(totalScore);
        submission.setScoreStatus("TEACHER_CONFIRMED");
        submissionMapper.updateById(submission);

        return Result.ok();
    }

    public Result<Void> publishScore(Long submissionId) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }
        submission.setScoreStatus("PUBLISHED");
        submissionMapper.updateById(submission);
        return Result.ok();
    }

    public Result<Void> returnSubmission(Long submissionId, String reason) {
        Submission submission = submissionMapper.selectById(submissionId);
        if (submission == null) {
            return Result.error(40401, "提交记录不存在");
        }
        submission.setScoreStatus("RETURNED");
        submissionMapper.updateById(submission);
        return Result.ok();
    }
}
