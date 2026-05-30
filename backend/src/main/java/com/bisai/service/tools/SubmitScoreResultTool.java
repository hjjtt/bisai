package com.bisai.service.tools;

import com.bisai.entity.Indicator;
import com.bisai.entity.ScoreResult;
import com.bisai.mapper.IndicatorMapper;
import com.bisai.mapper.ScoreResultMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.function.Function;

@Slf4j
@Component("submitScoreResultTool")
@Description("当你完成某一个指标的思考并决定分数后，调用此工具将该 indicatorId 的分数存入数据库。必须针对所有指标逐一调用。")
@RequiredArgsConstructor
public class SubmitScoreResultTool implements Function<SubmitScoreResultTool.Request, String> {

    private final ScoreResultMapper scoreResultMapper;
    private final IndicatorMapper indicatorMapper;
    private final ToolCallGuard toolCallGuard;

    public record Request(Long submissionId, Long indicatorId, double score, String reasoning) {}

    @Override
    public String apply(Request request) {
        toolCallGuard.checkAndRecord(request.submissionId(), "submitScoreResultTool");
        log.info("Agent 调用工具：SubmitScoreResultTool(submissionId={}, indicatorId={}, score={})",
                request.submissionId(), request.indicatorId(), request.score());
        try {
            // 防御：检查是否已经评分过
            Long count = scoreResultMapper.selectCount(
                    new LambdaQueryWrapper<ScoreResult>()
                            .eq(ScoreResult::getSubmissionId, request.submissionId())
                            .eq(ScoreResult::getIndicatorId, request.indicatorId())
            );
            if (count > 0) {
                return "失败：该指标 (indicatorId=" + request.indicatorId() + ") 已经被评分过了，请不要重复提交，继续评分下一个指标。";
            }

            Indicator ind = indicatorMapper.selectById(request.indicatorId());
            if (ind == null) return "失败：无效的 indicatorId";

            // 边界约束
            double maxScore = ind.getMaxScore() != null ? ind.getMaxScore().doubleValue() : 100.0;
            double clampedScore = Math.max(0, Math.min(request.score(), maxScore));

            ScoreResult sr = new ScoreResult();
            sr.setSubmissionId(request.submissionId());
            sr.setIndicatorId(request.indicatorId());
            sr.setAutoScore(BigDecimal.valueOf(clampedScore));
            sr.setReason(request.reasoning());
            sr.setIndicatorName(ind.getName());
            sr.setMaxScore(ind.getMaxScore());
            sr.setCreatedAt(LocalDateTime.now());
            sr.setUpdatedAt(LocalDateTime.now());
            scoreResultMapper.insert(sr);

            return "保存成功。该项分数为：" + clampedScore + " / " + maxScore + "。请继续评估其他指标，或者回复 'FINAL: 阅卷完成'";
        } catch (Exception e) {
            log.error("保存评分结果失败", e);
            return "保存失败：" + e.getMessage();
        }
    }
}
