package com.ruoyi.studyroom.ai.service;

import com.ruoyi.studyroom.ai.domain.AiKbChunk;
import com.ruoyi.studyroom.ai.domain.AiKbDocument;

import java.util.List;

/**
 * AI 知识库服务：文档入库 -> 切分 -> 向量化 -> 混合检索
 */
public interface AiKbService {

    /** 文档入库，返回入库 chunk 数 */
    int addDocument(String title, String content);

    /** 删除文档及其分片 */
    void deleteDocument(Long documentId);

    /** 文档列表 */
    List<AiKbDocument> listDocuments();

    /** 检索 TopK 分片（向量 + 关键词混合打分） */
    List<KbHit> retrieve(String query, int topK);

    /** 检索命中结果 */
    class KbHit {
        private Long chunkId;
        private String content;
        private double score;
        private String documentTitle;

        public KbHit(Long chunkId, String content, double score, String documentTitle) {
            this.chunkId = chunkId;
            this.content = content;
            this.score = score;
            this.documentTitle = documentTitle;
        }

        public Long getChunkId() { return chunkId; }
        public String getContent() { return content; }
        public double getScore() { return score; }
        public String getDocumentTitle() { return documentTitle; }
    }

}