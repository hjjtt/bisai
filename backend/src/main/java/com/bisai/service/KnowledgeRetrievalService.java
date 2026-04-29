package com.bisai.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bisai.entity.DocumentChunk;
import com.bisai.entity.KnowledgeBase;
import com.bisai.entity.KnowledgeDocument;
import com.bisai.entity.TrainingTask;
import com.bisai.mapper.DocumentChunkMapper;
import com.bisai.mapper.KnowledgeBaseMapper;
import com.bisai.mapper.KnowledgeDocumentMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final ModelScopeClient aiClient;
    private final ObjectMapper objectMapper;

    public String retrieveContext(TrainingTask task, String query, int limit) {
        if (task == null || task.getCourseId() == null || query == null || query.isBlank()) {
            return "";
        }
        try {
            KnowledgeBase kb = knowledgeBaseMapper.selectOne(
                    new LambdaQueryWrapper<KnowledgeBase>().eq(KnowledgeBase::getCourseId, task.getCourseId())
            );
            if (kb == null) return "";

            List<Long> documentIds = knowledgeDocumentMapper.selectList(
                    new LambdaQueryWrapper<KnowledgeDocument>()
                            .eq(KnowledgeDocument::getKnowledgeBaseId, kb.getId())
                            .eq(KnowledgeDocument::getEnabled, true)
                            .eq(KnowledgeDocument::getVectorStatus, "SUCCESS")
            ).stream().map(KnowledgeDocument::getId).toList();
            if (documentIds.isEmpty()) return "";

            List<DocumentChunk> chunks = documentChunkMapper.selectList(
                    new LambdaQueryWrapper<DocumentChunk>()
                            .in(DocumentChunk::getKnowledgeDocumentId, documentIds)
            );
            if (chunks.isEmpty()) return "";

            List<Double> queryEmbedding = aiClient.embedding(trim(query, 2000));
            return chunks.stream()
                    .filter(chunk -> chunk.getEmbedding() != null && !chunk.getEmbedding().isBlank())
                    .map(chunk -> new ScoredChunk(chunk, cosine(queryEmbedding, parseEmbedding(chunk.getEmbedding()))))
                    .filter(scored -> !Double.isNaN(scored.score))
                    .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                    .limit(limit)
                    .map(scored -> "- " + trim(scored.chunk.getContent(), 800))
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
        } catch (Exception e) {
            log.warn("知识库召回失败 taskId={}: {}", task.getId(), e.getMessage());
            return "";
        }
    }

    private List<Double> parseEmbedding(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    private double cosine(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return Double.NaN;
        int n = Math.min(a.size(), b.size());
        double dot = 0;
        double normA = 0;
        double normB = 0;
        for (int i = 0; i < n; i++) {
            double av = Objects.requireNonNullElse(a.get(i), 0.0);
            double bv = Objects.requireNonNullElse(b.get(i), 0.0);
            dot += av * bv;
            normA += av * av;
            normB += bv * bv;
        }
        if (normA == 0 || normB == 0) return Double.NaN;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String trim(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }

    private record ScoredChunk(DocumentChunk chunk, double score) {
    }
}
