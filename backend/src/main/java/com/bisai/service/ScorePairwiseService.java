package com.bisai.service;

import com.bisai.common.Result;
import com.bisai.entity.*;
import com.bisai.mapper.*;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * P2.8: Pairwise 比较服务
 *
 * 对同一任务下的多份提交进行两两比较排序，
 * 比独立评分更能区分相近质量的报告。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ScorePairwiseService {

    private final ScorePairwiseMapper pairwiseMapper;
    private final SubmissionMapper submissionMapper;
    private final TrainingTaskMapper taskMapper;
    private final UserMapper userMapper;
    private final IndicatorMapper indicatorMapper;
    private final ScoreResultMapper scoreResultMapper;
    private final ModelScopeClient aiClient;

    /**
     * 对同一任务下的所有提交执行 Pairwise 比较
     *
     * 策略：不是全排列（N²），而是只比较 AI 分数相邻的提交（N-1 次），
     * 侧重校验边界判定是否合理。
     *
     * @param taskId 任务 ID
     * @return 比较结果数量
     */
    public Result<Integer> compareAllForTask(Long taskId) {
        TrainingTask task = taskMapper.selectById(taskId);
        if (task == null) return Result.error(40401, "任务不存在");

        // 获取该任务下所有已评分的提交，按 AI 分数排序
        List<Submission> submissions = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>()
                        .eq(Submission::getTaskId, taskId)
                        .eq(Submission::getScoreStatus, "AI_SCORED")
                        .orderByAsc(Submission::getAutoTotalScore)
        );

        if (submissions.size() < 2) {
            return Result.error(40001, "至少需要 2 份已评分的提交才能比较");
        }

        // 批量查学生姓名
        Set<Long> studentIds = submissions.stream().map(Submission::getStudentId).collect(Collectors.toSet());
        Map<Long, String> nameMap = userMapper.selectList(
                new LambdaQueryWrapper<User>().in(User::getId, studentIds)
        ).stream().collect(Collectors.toMap(User::getId, u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));

        // 批量查评分结果
        List<Long> subIds = submissions.stream().map(Submission::getId).collect(Collectors.toList());
        Map<Long, List<ScoreResult>> scoreMap = scoreResultMapper.selectList(
                new LambdaQueryWrapper<ScoreResult>().in(ScoreResult::getSubmissionId, subIds)
        ).stream().collect(Collectors.groupingBy(ScoreResult::getSubmissionId));

        // 清除旧的比较结果
        pairwiseMapper.delete(
                new LambdaQueryWrapper<ScorePairwise>().eq(ScorePairwise::getTaskId, taskId)
        );

        // 获取任务要求
        String requirements = task.getRequirements() != null ? task.getRequirements() : "";

        // 获取评分指标摘要
        List<Indicator> indicators = indicatorMapper.selectList(
                new LambdaQueryWrapper<Indicator>()
                        .eq(Indicator::getTemplateId, task.getTemplateId())
                        .isNull(Indicator::getParentId)
                        .orderByAsc(Indicator::getSortOrder)
        );
        StringBuilder indicatorSummary = new StringBuilder();
        for (Indicator ind : indicators) {
            indicatorSummary.append("- ").append(ind.getName())
                    .append(" (满分 ").append(ind.getMaxScore()).append(")\n");
        }

        int comparisonCount = 0;

        // 相邻比较（N-1 次）
        for (int i = 0; i < submissions.size() - 1; i++) {
            Submission subA = submissions.get(i);
            Submission subB = submissions.get(i + 1);

            String nameA = nameMap.getOrDefault(subA.getStudentId(), "学生" + subA.getStudentId());
            String nameB = nameMap.getOrDefault(subB.getStudentId(), "学生" + subB.getStudentId());

            // 构建评分摘要
            String scoreSummaryA = buildScoreSummary(scoreMap.getOrDefault(subA.getId(), List.of()), nameA);
            String scoreSummaryB = buildScoreSummary(scoreMap.getOrDefault(subB.getId(), List.of()), nameB);

            try {
                String result = doPairwiseCompare(taskId, subA, subB, nameA, nameB,
                        requirements, indicatorSummary.toString(), scoreSummaryA, scoreSummaryB);
                comparisonCount++;
                log.info("Pairwise 比较: task={}, A={}({}分) vs B={}({}分) → {}",
                        taskId, nameA, subA.getAutoTotalScore(), nameB, subB.getAutoTotalScore(), result);
            } catch (Exception e) {
                log.warn("Pairwise 比较失败: A={} vs B={}: {}", subA.getId(), subB.getId(), e.getMessage());
                // 单对比较失败不影响其他
            }
        }

        return Result.ok(comparisonCount);
    }

    /**
     * 单对比较
     */
    private String doPairwiseCompare(Long taskId, Submission subA, Submission subB,
                                      String nameA, String nameB,
                                      String requirements, String indicators,
                                      String scoreSummaryA, String scoreSummaryB) {
        String systemPrompt = "你是实训成果对比评审专家。你有两份学生报告的评分摘要。\n" +
                "请比较哪份报告整体质量更高，给出判定和理由。\n\n" +
                "判定标准：\n" +
                "- A: A 报告整体更好\n" +
                "- B: B 报告整体更好\n" +
                "- TIE: 两份报告质量相当，难以区分\n\n" +
                "返回 JSON：{\"winner\":\"A/B/TIE\",\"reasoning\":\"比较理由(100字以内)\"}";

        String userMessage = "## 任务要求\n" + requirements + "\n\n" +
                "## 评分指标\n" + indicators + "\n" +
                "## 报告 A（" + nameA + "，总分: " + subA.getAutoTotalScore() + "）\n" +
                scoreSummaryA + "\n\n" +
                "## 报告 B（" + nameB + "，总分: " + subB.getAutoTotalScore() + "）\n" +
                scoreSummaryB;

        JsonNode result = aiClient.chatAsJson(systemPrompt, userMessage, 0.1);

        String winner = result.path("winner").asText("TIE").toUpperCase();
        if (!winner.equals("A") && !winner.equals("B")) winner = "TIE";
        String reasoning = result.path("reasoning").asText("");

        ScorePairwise pw = new ScorePairwise();
        pw.setTaskId(taskId);
        pw.setSubmissionAId(subA.getId());
        pw.setSubmissionBId(subB.getId());
        pw.setWinner(winner);
        pw.setReasoning(reasoning);
        pw.setModel("pairwise");
        pw.setCreatedAt(LocalDateTime.now());
        pairwiseMapper.insert(pw);

        return winner;
    }

    /**
     * 构建单份报告的评分摘要（用于 Pairwise Prompt）
     */
    private String buildScoreSummary(List<ScoreResult> scores, String studentName) {
        if (scores.isEmpty()) return studentName + "：无评分数据";
        StringBuilder sb = new StringBuilder(studentName + "：\n");
        for (ScoreResult sr : scores) {
            sb.append("  - ").append(sr.getIndicatorName() != null ? sr.getIndicatorName() : "指标" + sr.getIndicatorId());
            sb.append(": ").append(sr.getAutoScore()).append("分");
            if (sr.getReason() != null && !sr.getReason().isEmpty()) {
                sb.append(" (").append(sr.getReason(), 0, Math.min(sr.getReason().length(), 60)).append(")");
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * 获取任务的 Pairwise 比较结果
     */
    public Result<List<ScorePairwise>> getComparisons(Long taskId) {
        List<ScorePairwise> results = pairwiseMapper.selectList(
                new LambdaQueryWrapper<ScorePairwise>()
                        .eq(ScorePairwise::getTaskId, taskId)
                        .orderByAsc(ScorePairwise::getId)
        );

        // 填充学生姓名和分数
        Set<Long> allSubIds = new HashSet<>();
        for (ScorePairwise pw : results) {
            allSubIds.add(pw.getSubmissionAId());
            allSubIds.add(pw.getSubmissionBId());
        }
        if (!allSubIds.isEmpty()) {
            Map<Long, Submission> subMap = submissionMapper.selectList(
                    new LambdaQueryWrapper<Submission>().in(Submission::getId, allSubIds)
            ).stream().collect(Collectors.toMap(Submission::getId, s -> s));

            Set<Long> studentIds = subMap.values().stream()
                    .map(Submission::getStudentId).collect(Collectors.toSet());
            Map<Long, String> nameMap = userMapper.selectList(
                    new LambdaQueryWrapper<User>().in(User::getId, studentIds)
            ).stream().collect(Collectors.toMap(User::getId,
                    u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));

            for (ScorePairwise pw : results) {
                Submission subA = subMap.get(pw.getSubmissionAId());
                Submission subB = subMap.get(pw.getSubmissionBId());
                if (subA != null) {
                    pw.setStudentAName(nameMap.getOrDefault(subA.getStudentId(), ""));
                    pw.setScoreA(subA.getAutoTotalScore());
                }
                if (subB != null) {
                    pw.setStudentBName(nameMap.getOrDefault(subB.getStudentId(), ""));
                    pw.setScoreB(subB.getAutoTotalScore());
                }
            }
        }

        return Result.ok(results);
    }

    /**
     * 基于 Pairwise 结果生成排名
     */
    public Result<List<Map<String, Object>>> getRanking(Long taskId) {
        List<ScorePairwise> comparisons = pairwiseMapper.selectList(
                new LambdaQueryWrapper<ScorePairwise>().eq(ScorePairwise::getTaskId, taskId)
        );

        // 统计每个 submission 的胜场
        Map<Long, Integer> wins = new HashMap<>();
        Map<Long, Integer> losses = new HashMap<>();
        for (ScorePairwise pw : comparisons) {
            if ("A".equals(pw.getWinner())) {
                wins.merge(pw.getSubmissionAId(), 1, Integer::sum);
                losses.merge(pw.getSubmissionBId(), 1, Integer::sum);
            } else if ("B".equals(pw.getWinner())) {
                wins.merge(pw.getSubmissionBId(), 1, Integer::sum);
                losses.merge(pw.getSubmissionAId(), 1, Integer::sum);
            }
        }

        // 获取所有涉及的提交
        Set<Long> subIds = new HashSet<>();
        for (ScorePairwise pw : comparisons) {
            subIds.add(pw.getSubmissionAId());
            subIds.add(pw.getSubmissionBId());
        }

        if (subIds.isEmpty()) return Result.ok(List.of());

        Map<Long, Submission> subMap = submissionMapper.selectList(
                new LambdaQueryWrapper<Submission>().in(Submission::getId, subIds)
        ).stream().collect(Collectors.toMap(Submission::getId, s -> s));

        Set<Long> studentIds = subMap.values().stream()
                .map(Submission::getStudentId).collect(Collectors.toSet());
        Map<Long, String> nameMap = userMapper.selectList(
                new LambdaQueryWrapper<User>().in(User::getId, studentIds)
        ).stream().collect(Collectors.toMap(User::getId,
                u -> u.getRealName() != null ? u.getRealName() : u.getUsername()));

        // 构建排名（按净胜场降序）
        List<Map<String, Object>> ranking = new ArrayList<>();
        for (Long subId : subIds) {
            Submission sub = subMap.get(subId);
            if (sub == null) continue;
            int w = wins.getOrDefault(subId, 0);
            int l = losses.getOrDefault(subId, 0);
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("submissionId", subId);
            entry.put("studentName", nameMap.getOrDefault(sub.getStudentId(), ""));
            entry.put("aiScore", sub.getAutoTotalScore());
            entry.put("wins", w);
            entry.put("losses", l);
            entry.put("netWins", w - l);
            ranking.add(entry);
        }

        ranking.sort((a, b) -> ((Integer) b.get("netWins")).compareTo((Integer) a.get("netWins")));

        // 添加排名序号
        for (int i = 0; i < ranking.size(); i++) {
            ranking.get(i).put("rank", i + 1);
        }

        return Result.ok(ranking);
    }
}
