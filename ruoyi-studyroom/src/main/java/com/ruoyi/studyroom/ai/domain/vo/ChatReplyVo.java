package com.ruoyi.studyroom.ai.domain.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 对话回复
 * 同时返回工具调用轨迹与知识库命中，便于前端展示与评测留痕
 */
@Data
public class ChatReplyVo {

    /** 会话 ID */
    private Long sessionId;

    /** 回复文本 */
    private String reply;

    /** 工具调用轨迹 */
    private List<ToolTraceVo> toolTrace = new ArrayList<>();

    /** 知识库命中 */
    private List<KbHitVo> kbHits = new ArrayList<>();

    /** 输入 token */
    private Integer promptTokens = 0;

    /** 输出 token */
    private Integer completionTokens = 0;

    /** 模型名 */
    private String model;

    /** 端到端延迟 ms */
    private Long latencyMs;

    /** 是否 Mock 模式 */
    private Boolean mock;

    @Data
    public static class ToolTraceVo {
        private String name;
        private String arguments;
        private String result;
    }

    @Data
    public static class KbHitVo {
        private String title;
        private String content;
        private Double score;
    }

}