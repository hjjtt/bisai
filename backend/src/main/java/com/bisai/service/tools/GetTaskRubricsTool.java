package com.bisai.service.tools;

import com.bisai.entity.Indicator;
import com.bisai.entity.TrainingTask;
import com.bisai.mapper.IndicatorMapper;
import com.bisai.mapper.TrainingTaskMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.function.Function;

@Slf4j
@Component("getTaskRubricsTool")
@Description("根据 taskId 获取当前任务的要求和所有的评分细则指标(Indicators)。在批改之前必须调用此工具了解满分标准和权重。")
@RequiredArgsConstructor
public class GetTaskRubricsTool implements Function<GetTaskRubricsTool.Request, String> {

    private final TrainingTaskMapper taskMapper;
    private final IndicatorMapper indicatorMapper;
    private final ToolCallGuard toolCallGuard;

    public record Request(Long taskId) {}

    @Override
    public String apply(Request request) {
        toolCallGuard.checkAndRecord(null, "getTaskRubricsTool");
        log.info("Agent 调用工具：GetTaskRubricsTool(taskId={})", request.taskId());
        try {
            TrainingTask task = taskMapper.selectById(request.taskId());
            if (task == null || task.getTemplateId() == null) {
                return "未找到对应任务或任务没有配置评分模板";
            }
            
            List<Indicator> indicators = indicatorMapper.selectList(
                    new LambdaQueryWrapper<Indicator>()
                            .eq(Indicator::getTemplateId, task.getTemplateId())
                            .orderByAsc(Indicator::getSortOrder)
            );
            
            StringBuilder sb = new StringBuilder();
            sb.append("【任务要求】\n").append(task.getRequirements()).append("\n\n");
            sb.append("【评分细则】\n");
            for (Indicator ind : indicators) {
                sb.append("- IndicatorID: ").append(ind.getId())
                  .append(", 名称: ").append(ind.getName())
                  .append(", 满分: ").append(ind.getMaxScore())
                  .append(", 权重: ").append(ind.getWeight())
                  .append(", 父指标ID: ").append(ind.getParentId() == null ? "无" : ind.getParentId());
                if (ind.getScoreRule() != null) {
                    sb.append(", 评分规则: ").append(ind.getScoreRule());
                }
                sb.append("\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.error("获取评分细则失败", e);
            return "获取评分细则失败：" + e.getMessage();
        }
    }
}
