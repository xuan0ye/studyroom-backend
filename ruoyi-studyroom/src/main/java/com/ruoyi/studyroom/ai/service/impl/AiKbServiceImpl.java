package com.ruoyi.studyroom.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.studyroom.ai.config.StudyroomAiProperties;
import com.ruoyi.studyroom.ai.domain.AiKbChunk;
import com.ruoyi.studyroom.ai.domain.AiKbDocument;
import com.ruoyi.studyroom.ai.mapper.AiKbChunkMapper;
import com.ruoyi.studyroom.ai.mapper.AiKbDocumentMapper;
import com.ruoyi.studyroom.ai.service.AiKbService;
import com.ruoyi.studyroom.ai.service.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库实现：切分（按标点/长度，带重叠）+ 向量化 + 余弦&关键词混合检索
 * 小规模场景直接在内存中计算，数据量上来后可平滑替换为向量数据库（如 pgvector/Milvus）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiKbServiceImpl implements AiKbService {

    private final AiKbDocumentMapper documentMapper;
    private final AiKbChunkMapper chunkMapper;
    private final LlmClient llmClient;
    private final StudyroomAiProperties props;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addDocument(String title, String content) {
        AiKbDocument doc = new AiKbDocument();
        doc.setTitle(title);
        doc.setContent(content);
        doc.setSourceType("manual");
        doc.setStatus(1);
        documentMapper.insert(doc);

        List<String> chunks = split(content, props.getKbChunkSize(), props.getKbChunkOverlap());
        List<float[]> vectors;
        try {
            vectors = llmClient.embedBatch(chunks);
        } catch (Exception e) {
            log.warn("[AI-KB] embedding 失败，回退 Mock 向量: {}", e.getMessage());
            vectors = new ArrayList<>();
            for (String c : chunks) {
                vectors.add(llmClient.embed(c));
            }
        }
        int idx = 0;
        for (String chunk : chunks) {
            AiKbChunk entity = new AiKbChunk();
            entity.setDocumentId(doc.getId());
            entity.setChunkIndex(idx);
            entity.setContent(chunk);
            entity.setEmbeddingJson(JSONUtil.toJsonStr(vectors.get(idx)));
            entity.setTokens(estimateTokens(chunk));
            chunkMapper.insert(entity);
            idx++;
        }
        doc.setChunkCount(idx);
        documentMapper.updateById(doc);
        return idx;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDocument(Long documentId) {
        chunkMapper.delete(new LambdaQueryWrapper<AiKbChunk>().eq(AiKbChunk::getDocumentId, documentId));
        documentMapper.deleteById(documentId);
    }

    @Override
    public List<AiKbDocument> listDocuments() {
        return documentMapper.selectList(new LambdaQueryWrapper<AiKbDocument>().orderByDesc(AiKbDocument::getCreateTime));
    }

    @Override
    public List<KbHit> retrieve(String query, int topK) {
        List<AiKbChunk> chunks = chunkMapper.selectList(new LambdaQueryWrapper<AiKbChunk>());
        if (chunks.isEmpty()) {
            return new ArrayList<>();
        }
        float[] queryVec;
        try {
            queryVec = llmClient.embed(query);
        } catch (Exception e) {
            log.warn("[AI-KB] 检索向量化失败，仅按关键词检索: {}", e.getMessage());
            queryVec = null;
        }
        Map<String, String> docTitles = new HashMap<>();
        for (AiKbDocument d : documentMapper.selectList(new LambdaQueryWrapper<AiKbDocument>())) {
            docTitles.put(String.valueOf(d.getId()), d.getTitle());
        }
        Map<String, Double> scores = new HashMap<>();
        for (AiKbChunk chunk : chunks) {
            double score = 0;
            if (queryVec != null && StrUtil.isNotBlank(chunk.getEmbeddingJson())) {
                float[] chunkVec = parseVector(chunk.getEmbeddingJson());
                score += 0.7 * cosine(queryVec, chunkVec);
            }
            score += 0.3 * keywordScore(query, chunk.getContent());
            scores.put(String.valueOf(chunk.getId()), score);
        }
        List<KbHit> hits = new ArrayList<>();
        for (AiKbChunk chunk : chunks) {
            hits.add(new KbHit(chunk.getId(), chunk.getContent(), scores.get(String.valueOf(chunk.getId())),
                docTitles.getOrDefault(String.valueOf(chunk.getDocumentId()), "未命名文档")));
        }
        hits.sort(Comparator.comparingDouble(KbHit::getScore).reversed());
        return hits.subList(0, Math.min(topK, hits.size()));
    }

    /**
     * 切分：按段落/句号/长度切分，chunk 之间保留重叠，避免打断语义
     */
    private List<String> split(String content, int size, int overlap) {
        List<String> result = new ArrayList<>();
        if (StrUtil.isBlank(content)) {
            return result;
        }
        String text = content.replace("\r\n", "\n").replace("\r", "\n");
        String[] paragraphs = text.split("\n+");
        StringBuilder current = new StringBuilder();
        for (String p : paragraphs) {
            String para = p.trim();
            if (StrUtil.isBlank(para)) {
                continue;
            }
            if (current.length() + para.length() > size && current.length() > 0) {
                result.add(current.toString());
                current = new StringBuilder();
            }
            current.append(para).append("\n");
        }
        if (current.length() > 0) {
            result.add(current.toString());
        }
        // 超长段落按句切分
        List<String> refined = new ArrayList<>();
        for (String chunk : result) {
            if (chunk.length() <= size) {
                refined.add(chunk);
                continue;
            }
            int start = 0;
            while (start < chunk.length()) {
                int end = Math.min(start + size, chunk.length());
                int cut = chunk.lastIndexOf("。", end);
                if (cut > start + size / 2) {
                    end = cut + 1;
                }
                String sub = chunk.substring(start, end).trim();
                if (StrUtil.isNotBlank(sub)) {
                    refined.add(sub);
                }
                start = Math.max(end - overlap, start + 1);
            }
        }
        return refined;
    }

    private int estimateTokens(String text) {
        // 粗略估算：中文按字符、英文按单词
        if (text == null) {
            return 0;
        }
        int cjk = 0;
        int other = 0;
        for (char c : text.toCharArray()) {
            if (Character.UnicodeScript.of(c) == Character.UnicodeScript.HAN) {
                cjk++;
            } else if (!Character.isWhitespace(c)) {
                other++;
            }
        }
        return cjk + (int) Math.ceil(other / 3.0);
    }

    private float[] parseVector(String json) {
        JSONArray arr = JSONUtil.parseArray(json);
        float[] v = new float[arr.size()];
        for (int i = 0; i < arr.size(); i++) {
            v[i] = arr.getFloat(i, 0F);
        }
        return v;
    }

    private double cosine(float[] a, float[] b) {
        if (a.length != b.length || a.length == 0) {
            return 0;
        }
        double dot = 0;
        double na = 0;
        double nb = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            na += a[i] * a[i];
            nb += b[i] * b[i];
        }
        if (na == 0 || nb == 0) {
            return 0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    /**
     * 关键词打分：分词（简单中文二元切分）后在正文中的覆盖率
     */
    private double keywordScore(String query, String content) {
        if (StrUtil.isBlank(query) || StrUtil.isBlank(content)) {
            return 0;
        }
        String q = query.replaceAll("\\s+", "");
        String c = content.replaceAll("\\s+", "");
        if (q.length() == 0) {
            return 0;
        }
        double hit = 0;
        // 单字 + 双字 n-gram 命中率
        for (int i = 0; i < q.length(); i++) {
            String ch = q.substring(i, i + 1);
            if (c.contains(ch)) {
                hit++;
            }
            if (i + 1 < q.length()) {
                String bg = q.substring(i, i + 2);
                if (c.contains(bg)) {
                    hit += 1.5;
                }
            }
        }
        double max = q.length() * 2.5;
        return max == 0 ? 0 : hit / max;
    }

}