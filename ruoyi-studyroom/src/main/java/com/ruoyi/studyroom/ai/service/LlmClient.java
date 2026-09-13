package com.ruoyi.studyroom.ai.service;

import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ruoyi.studyroom.ai.config.StudyroomAiProperties;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * OpenAI 兼容的大模型客户端（chat completions + embeddings）
 * 无 API Key 时自动进入 Mock 模式：embedding 退化为词袋哈希向量，chat 由上层规则兜底
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LlmClient {

    private static final int MOCK_EMBED_DIM = 256;

    private final StudyroomAiProperties props;

    /** 是否 Mock 模式（无 Key / 关闭） */
    public boolean isMock() {
        return !Boolean.TRUE.equals(props.getEnabled()) || props.isMock();
    }

    /**
     * 调用对话模型
     *
     * @param messages 消息列表 [{role, content}]
     * @param toolsJson OpenAI tools 定义 JSON 数组字符串，可为 null
     */
    public LlmChatResult chat(List<JSONObject> messages, String toolsJson) {
        JSONObject body = JSONUtil.createObj()
            .set("model", props.getChatModel())
            .set("messages", messages)
            .set("temperature", props.getTemperature())
            .set("max_tokens", props.getMaxTokens())
            .set("stream", false);
        if (StrUtil.isNotBlank(toolsJson)) {
            body.set("tools", JSONUtil.parseArray(toolsJson));
            body.set("tool_choice", "auto");
        }
        String resp = HttpRequest.post(props.getBaseUrl() + "/chat/completions")
            .header("Authorization", "Bearer " + props.getApiKey())
            .header("Content-Type", "application/json")
            .timeout(props.getTimeoutSec() * 1000)
            .body(body.toString())
            .execute()
            .body();
        JSONObject root = JSONUtil.parseObj(resp);
        if (root.containsKey("error")) {
            JSONObject err = root.getJSONObject("error");
            throw new RuntimeException("LLM 接口错误: " + err.getStr("message", String.valueOf(root)));
        }
        LlmChatResult result = new LlmChatResult();
        result.setModel(props.getChatModel());
        JSONObject usage = root.getJSONObject("usage");
        if (usage != null) {
            result.setPromptTokens(usage.getInt("prompt_tokens", 0));
            result.setCompletionTokens(usage.getInt("completion_tokens", 0));
        }
        JSONArray choices = root.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject message = choices.getJSONObject(0).getJSONObject("message");
            if (message != null) {
                result.setContent(message.getStr("content"));
                JSONArray toolCalls = message.getJSONArray("tool_calls");
                if (toolCalls != null) {
                    List<ToolCall> calls = new ArrayList<>();
                    for (Object item : toolCalls) {
                        JSONObject tc = (JSONObject) item;
                        JSONObject fn = tc.getJSONObject("function");
                        if (fn != null) {
                            ToolCall call = new ToolCall();
                            call.setId(tc.getStr("id"));
                            call.setName(fn.getStr("name"));
                            call.setArguments(fn.getStr("arguments", "{}"));
                            calls.add(call);
                        }
                    }
                    result.setToolCalls(calls);
                }
            }
        }
        return result;
    }

    /**
     * 文本向量化
     */
    public float[] embed(String text) {
        return embedBatch(java.util.Collections.singletonList(text)).get(0);
    }

    /**
     * 批量文本向量化；Mock 模式下返回确定性的词袋哈希向量
     */
    public List<float[]> embedBatch(List<String> texts) {
        List<float[]> vectors = new ArrayList<>();
        if (isMock()) {
            for (String text : texts) {
                vectors.add(mockEmbed(text));
            }
            return vectors;
        }
        JSONArray input = new JSONArray();
        for (String text : texts) {
            input.add(text);
        }
        JSONObject body = JSONUtil.createObj()
            .set("model", props.getEmbeddingModel())
            .set("input", input);
        String resp = HttpRequest.post(props.getBaseUrl() + "/embeddings")
            .header("Authorization", "Bearer " + props.getApiKey())
            .header("Content-Type", "application/json")
            .timeout(props.getTimeoutSec() * 1000)
            .body(body.toString())
            .execute()
            .body();
        JSONObject root = JSONUtil.parseObj(resp);
        if (root.containsKey("error")) {
            JSONObject err = root.getJSONObject("error");
            throw new RuntimeException("Embedding 接口错误: " + err.getStr("message", String.valueOf(root)));
        }
        JSONArray data = root.getJSONArray("data");
        if (data == null) {
            throw new RuntimeException("Embedding 接口返回异常: " + resp);
        }
        for (Object item : data) {
            JSONObject d = (JSONObject) item;
            JSONArray emb = d.getJSONArray("embedding");
            float[] v = new float[emb.size()];
            for (int i = 0; i < emb.size(); i++) {
                v[i] = emb.getFloat(i, 0F);
            }
            vectors.add(v);
        }
        return vectors;
    }

    /**
     * Mock embedding：字符/双字/三字 n-gram 哈希到固定维度并归一化
     */
    private float[] mockEmbed(String text) {
        float[] v = new float[MOCK_EMBED_DIM];
        if (text == null) {
            return v;
        }
        String norm = text.replaceAll("\\s+", "");
        for (int i = 0; i < norm.length(); i++) {
            addHash(v, norm.substring(i, i + 1));
            if (i + 1 < norm.length()) {
                addHash(v, norm.substring(i, i + 2));
            }
            if (i + 2 < norm.length()) {
                addHash(v, norm.substring(i, i + 3));
            }
        }
        // 归一化
        double sum = 0;
        for (float f : v) {
            sum += f * f;
        }
        if (sum > 0) {
            double inv = 1.0 / Math.sqrt(sum);
            for (int i = 0; i < v.length; i++) {
                v[i] = (float) (v[i] * inv);
            }
        }
        return v;
    }

    private void addHash(float[] v, String s) {
        byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
        int h = 17;
        for (byte b : bytes) {
            h = h * 31 + b;
        }
        // 无符号化，配合随机符号让相似文本方向更接近
        int idx = Math.floorMod(h, MOCK_EMBED_DIM);
        int sign = (h & 1) == 0 ? 1 : -1;
        v[idx] += sign;
    }

    /**
     * 对话模型返回结果
     */
    @Data
    public static class LlmChatResult {
        private String content;
        private List<ToolCall> toolCalls = new ArrayList<>();
        private Integer promptTokens = 0;
        private Integer completionTokens = 0;
        private String model;
    }

    @Data
    public static class ToolCall {
        private String id;
        private String name;
        private String arguments;
    }

    /** 便于自测的随机数（保留，避免未来扩展时重复引入） */
    public static int randomInt(int bound) {
        return ThreadLocalRandom.current().nextInt(bound);
    }

}