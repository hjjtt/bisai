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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class KnowledgeRetrievalService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;
    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final DocumentChunkMapper documentChunkMapper;
    private final ModelScopeClient aiClient;
    private final ObjectMapper objectMapper;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.base-url:https://api-inference.modelscope.cn/v1}")
    private String baseUrl;

    @Value("${ai.rerank-model:}")
    private String rerankModel;

    private static final int RERANK_TOP_K = 20;
    private static final int RERANK_LIMIT = 5;

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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

            // 第一阶段：向量检索
            List<Double> queryEmbedding = aiClient.embedding(trim(query, 2000));
            List<ScoredChunk> vectorResults = chunks.stream()
                    .filter(chunk -> chunk.getEmbedding() != null && !chunk.getEmbedding().isBlank())
                    .map(chunk -> new ScoredChunk(chunk, cosine(queryEmbedding, parseEmbedding(chunk.getEmbedding()))))
                    .filter(scored -> !Double.isNaN(scored.score))
                    .sorted(Comparator.comparingDouble(ScoredChunk::score).reversed())
                    .limit(RERANK_TOP_K)
                    .toList();

            if (vectorResults.isEmpty()) return "";

            // 第二阶段：Rerank 重排（如果配置了 Rerank 模型）
            List<ScoredChunk> finalResults;
            if (rerankModel != null && !rerankModel.isBlank()) {
                finalResults = rerank(query, vectorResults);
            } else {
                finalResults = vectorResults;
            }

            return finalResults.stream()
                    .limit(limit)
                    .map(scored -> "- " + trim(scored.chunk.getContent(), 800))
                    .reduce((a, b) -> a + "\n" + b)
                    .orElse("");
        } catch (Exception e) {
            log.warn("知识库召回失败 taskId={}: {}", task.getId(), e.getMessage());
            return "";
        }
    }

    /**
     * 使用 Rerank 模型对向量检索结果进行重排
     */
    private List<ScoredChunk> rerank(String query, List<ScoredChunk> candidates) {
        if (candidates.isEmpty()) return candidates;

        try {
            List<String> texts = candidates.stream()
                    .map(c -> trim(c.chunk.getContent(), 1000))
                    .toList();

            String body = objectMapper.writeValueAsString(new RerankRequest(query, texts, rerankModel));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/rerank"))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .timeout(Duration.ofSeconds(15))
                    .build();

            HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode results = root.path("results");

            if (results.isArray() && results.size() > 0) {
                List<ScoredChunk> reranked = new ArrayList<>();
                for (JsonNode r : results) {
                    int index = r.path("index").asInt(0);
                    double score = r.path("relevance_score").asDouble(0);
                    if (index < candidates.size()) {
                        ScoredChunk original = candidates.get(index);
                        reranked.add(new ScoredChunk(original.chunk, score));
                    }
                }
                reranked.sort(Comparator.comparingDouble(ScoredChunk::score).reversed());
                log.info("Rerank 完成，原始{}条，重排后前{}条", candidates.size(), Math.min(reranked.size(), RERANK_LIMIT));
                return reranked;
            }
        } catch (Exception e) {
            log.warn("Rerank 调用失败，降级为向量排序: {}", e.getMessage());
        }

        return candidates;
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

    private record ScoredChunk(DocumentChunk chunk, double score) {}

    private record RerankRequest(String query, List<String> documents, String model) {}
}
