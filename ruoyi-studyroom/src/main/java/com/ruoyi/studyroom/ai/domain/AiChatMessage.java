package com.ruoyi.studyroom.ai.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 对话消息
 * 完整留痕：角色/内容/工具调用/知识库命中/用量/延迟/用户反馈，
 * 支撑「模型训练-业务应用-用户反馈-数据迭代」数据飞轮
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_chat_message")
public class AiChatMessage extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(value = "id")
    private Long id;

    /** 会话 ID */
    private Long sessionId;

    /** 用户 ID */
    private Long userId;

    /** 角色：user / assistant / tool */
    private String role;

    /** 消息内容 */
    private String content;

    /** 工具调用参数 JSON（assistant 消息） */
    private String toolCallsJson;

    /** 工具执行结果 JSON（tool 消息） */
    private String toolResultsJson;

    /** 使用的模型 */
    private String model;

    /** 输入 token */
    private Integer promptTokens;

    /** 输出 token */
    private Integer completionTokens;

    /** 端到端延迟 ms */
    private Integer latencyMs;

    /** 用户反馈：1-赞 -1-踩 0-中性 */
    private Integer rating;

    /** 用户反馈备注 */
    private String ratingComment;

    /** 命中的知识库 chunk（JSON 数组，留痕用） */
    private String kbHitsJson;

}