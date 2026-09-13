package com.ruoyi.studyroom.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * AI 学伴配置
 * 支持任意 OpenAI 兼容接口（OpenAI / DeepSeek / 智谱 / 本地 vLLM 等）
 * apiKey 为空时自动进入 Mock 模式（规则兜底，便于无 Key 联调与演示）
 * 由 StudyroomAiConfiguration 通过 @EnableConfigurationProperties 注册绑定
 */
@Data
@ConfigurationProperties(prefix = "studyroom.ai")
public class StudyroomAiProperties {

    /** 总开关 */
    private Boolean enabled = true;

    /** OpenAI 兼容接口地址，如 https://api.openai.com/v1 或 https://api.deepseek.com/v1 */
    private String baseUrl = "https://api.openai.com/v1";

    /** API Key，为空则启用 Mock 模式 */
    private String apiKey = "";

    /** 对话模型 */
    private String chatModel = "gpt-4o-mini";

    /** 向量模型 */
    private String embeddingModel = "text-embedding-3-small";

    /** 采样温度 */
    private Double temperature = 0.7;

    /** 单次最大输出 token */
    private Integer maxTokens = 1024;

    /** HTTP 超时（秒） */
    private Integer timeoutSec = 60;

    /** 携带的历史对话轮数 */
    private Integer historyTurns = 3;

    /** 知识库是否启用 */
    private Boolean kbEnabled = true;

    /** 检索 TopK */
    private Integer kbTopK = 4;

    /** 知识库切分大小（字符） */
    private Integer kbChunkSize = 200;

    /** 知识库切分重叠（字符） */
    private Integer kbChunkOverlap = 50;

    public boolean isMock() {
        return apiKey == null || apiKey.trim().isEmpty();
    }

}