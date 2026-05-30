package com.bisai.service.tools;

import com.bisai.entity.TrainingTask;
import com.bisai.mapper.TrainingTaskMapper;
import com.bisai.service.KnowledgeRetrievalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Description;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Slf4j
@Component("searchKnowledgeBaseTool")
@Description("如果批改中遇到不确定的专业知识，传入相关的查询关键词 query 去搜索 RAG 知识库，获取参考答案或技术文档。")
@RequiredArgsConstructor
public class SearchKnowledgeBaseTool implements Function<SearchKnowledgeBaseTool.Request, String> {

    private final TrainingTaskMapper taskMapper;
    private final KnowledgeRetrievalService knowledgeRetrievalService;
    private final ToolCallGuard toolCallGuard;

    public record Request(Long taskId, String query) {}

    @Override
    public String apply(Request request) {
        toolCallGuard.checkAndRecord(null, "searchKnowledgeBaseTool");
        log.info("Agent 调用工具：SearchKnowledgeBaseTool(taskId={}, query={})", request.taskId(), request.query());
        try {
            TrainingTask task = taskMapper.selectById(request.taskId());
            if (task == null) return "任务不存在";
            
            String context = knowledgeRetrievalService.retrieveContext(task, request.query(), 3);
            if (context == null || context.isEmpty()) {
                return "未检索到相关的知识库内容";
            }
            return context;
        } catch (Exception e) {
            log.error("搜索知识库失败", e);
            return "搜索失败：" + e.getMessage();
        }
    }
}
