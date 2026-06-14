package com.bisai.service;

import com.bisai.common.PageResult;
import com.bisai.common.Result;
import com.bisai.dto.PageQuery;
import com.bisai.entity.EvaluationTemplate;
import com.bisai.entity.Indicator;
import com.bisai.mapper.EvaluationTemplateMapper;
import com.bisai.mapper.IndicatorMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EvaluationTemplateService {

    private final EvaluationTemplateMapper templateMapper;
    private final IndicatorMapper indicatorMapper;
    private final com.bisai.mapper.TrainingTaskMapper taskMapper;

    public boolean isOwner(Long templateId, Long userId) {
        EvaluationTemplate template = templateMapper.selectById(templateId);
        return template != null && userId.equals(template.getCreatorId());
    }

    public Result<PageResult<EvaluationTemplate>> listTemplates(PageQuery query) {
        Page<EvaluationTemplate> page = new Page<>(query.getPage(), query.getSize());
        LambdaQueryWrapper<EvaluationTemplate> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByDesc(EvaluationTemplate::getCreatedAt);

        Page<EvaluationTemplate> result = templateMapper.selectPage(page, wrapper);
        return Result.ok(new PageResult<>(result.getRecords(), result.getCurrent(), result.getSize(), result.getTotal()));
    }

    public Result<EvaluationTemplate> getTemplate(Long id) {
        EvaluationTemplate template = templateMapper.selectById(id);
        if (template == null) {
            return Result.error(40401, "模板不存在");
        }

        // 查询指标
        List<Indicator> indicators = indicatorMapper.selectList(
                new LambdaQueryWrapper<Indicator>()
                        .eq(Indicator::getTemplateId, id)
                        .orderByAsc(Indicator::getSortOrder)
        );
        template.setIndicators(indicators);
        return Result.ok(template);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<EvaluationTemplate> createTemplate(EvaluationTemplate template) {
        template.setStatus("ENABLED");
        templateMapper.insert(template);

        // 保存指标
        saveIndicators(template.getId(), template.getIndicators());
        return Result.ok(template);
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<EvaluationTemplate> updateTemplate(Long id, EvaluationTemplate template) {
        EvaluationTemplate existing = templateMapper.selectById(id);
        if (existing == null) {
            return Result.error(40401, "模板不存在");
        }

        template.setId(id);
        templateMapper.updateById(template);

        // 更新指标：先删除旧指标，再插入新指标
        if (template.getIndicators() != null) {
            indicatorMapper.delete(
                    new LambdaQueryWrapper<Indicator>().eq(Indicator::getTemplateId, id)
            );
            saveIndicators(id, template.getIndicators());
        }

        return Result.ok(templateMapper.selectById(id));
    }

    @Transactional(rollbackFor = Exception.class)
    public Result<Void> deleteTemplate(Long id) {
        EvaluationTemplate existing = templateMapper.selectById(id);
        if (existing == null) {
            return Result.error(40401, "模板不存在");
        }

        // 检查是否被任务引用：DRAFT/PUBLISHED/CLOSED 状态的任务仍需该模板评分，删除会导致评分静默失效
        long taskCount = taskMapper.selectCount(
                new LambdaQueryWrapper<com.bisai.entity.TrainingTask>()
                        .eq(com.bisai.entity.TrainingTask::getTemplateId, id)
                        .in(com.bisai.entity.TrainingTask::getStatus, "DRAFT", "PUBLISHED", "CLOSED")
        );
        if (taskCount > 0) {
            return Result.error(40901, "该模板正被 " + taskCount + " 个任务引用，无法删除（请先解除关联或将任务归档）");
        }

        // 逻辑删除模板
        templateMapper.deleteById(id);

        // 删除关联的指标
        indicatorMapper.delete(
                new LambdaQueryWrapper<Indicator>().eq(Indicator::getTemplateId, id)
        );

        return Result.ok();
    }

    /**
     * 保存指标列表
     */
    private void saveIndicators(Long templateId, List<Indicator> indicators) {
        if (indicators == null || indicators.isEmpty()) {
            return;
        }
        for (int i = 0; i < indicators.size(); i++) {
            Indicator indicator = indicators.get(i);
            indicator.setId(null); // 确保新增
            indicator.setTemplateId(templateId);
            indicator.setSortOrder(i + 1);
            indicatorMapper.insert(indicator);

            // 保存子指标
            if (indicator.getChildren() != null && !indicator.getChildren().isEmpty()) {
                saveChildIndicators(templateId, indicator.getId(), indicator.getChildren(), 1);
            }
        }
    }

    /**
     * 递归保存子指标
     */
    private void saveChildIndicators(Long templateId, Long parentId, List<Indicator> children, int startOrder) {
        for (int i = 0; i < children.size(); i++) {
            Indicator child = children.get(i);
            child.setId(null);
            child.setTemplateId(templateId);
            child.setParentId(parentId);
            child.setSortOrder(startOrder + i);
            indicatorMapper.insert(child);

            if (child.getChildren() != null && !child.getChildren().isEmpty()) {
                saveChildIndicators(templateId, child.getId(), child.getChildren(), 1);
            }
        }
    }
}
