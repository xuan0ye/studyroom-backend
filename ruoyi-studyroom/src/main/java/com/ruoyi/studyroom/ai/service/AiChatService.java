package com.ruoyi.studyroom.ai.service;

import com.ruoyi.studyroom.ai.domain.AiChatSession;
import com.ruoyi.studyroom.ai.domain.vo.ChatReplyVo;

import java.util.List;
import java.util.Map;

/**
 * AI 对话服务：会话管理 / 对话编排（RAG + 工具调用循环）/ 反馈留痕 / 统计
 */
public interface AiChatService {

    /** 发起一轮对话 */
    ChatReplyVo chat(Long userId, Long sessionId, String message);

    /** 会话列表 */
    List<AiChatSession> listSessions(Long userId);

    /** 会话消息列表（供前端回放） */
    List<Map<String, Object>> listMessages(Long userId, Long sessionId);

    /** 保存用户反馈（数据飞轮：反馈回流训练数据） */
    void feedback(Long userId, Long messageId, Integer rating, String comment);

    /** 统计：会话数/消息数/好评率/工具调用数/平均延迟 */
    Map<String, Object> stats(Long userId);

}