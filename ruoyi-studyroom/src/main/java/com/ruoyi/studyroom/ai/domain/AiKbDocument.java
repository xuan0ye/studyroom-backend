package com.ruoyi.studyroom.ai.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 知识库文档
 * 数据飞轮：知识来源登记（手册/规则/笔记），支持版本与质量追踪
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_kb_document")
public class AiKbDocument extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 标题 */
    private String title;

    /** 原始正文 */
    private String content;

    /** 来源类型：manual-手工录入 file-文件导入 */
    private String sourceType;

    /** 切分后的 chunk 数量 */
    private Integer chunkCount;

    /** 状态：1-生效 0-停用 */
    private Integer status;

}