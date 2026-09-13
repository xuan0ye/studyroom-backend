package com.ruoyi.studyroom.ai.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 知识库分片
 * 每条 chunk 保存 embedding（JSON 数组文本），检索时做混合打分
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_kb_chunk")
public class AiKbChunk extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 所属文档 */
    private Long documentId;

    /** 分片序号 */
    private Integer chunkIndex;

    /** 分片正文 */
    private String content;

    /** embedding 向量（JSON 数组字符串） */
    private String embeddingJson;

    /** 预估 token 数 */
    private Integer tokens;

}