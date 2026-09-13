package com.ruoyi.studyroom.ai.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.domain.model.XcxLoginUser;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.studyroom.ai.config.StudyroomAiProperties;
import com.ruoyi.studyroom.ai.domain.AiChatMessage;
import com.ruoyi.studyroom.ai.domain.AiChatSession;
import com.ruoyi.studyroom.ai.domain.vo.ChatReplyVo;
import com.ruoyi.studyroom.ai.mapper.AiChatMessageMapper;
import com.ruoyi.studyroom.ai.mapper.AiChatSessionMapper;
import com.ruoyi.studyroom.ai.service.AiChatService;
import com.ruoyi.studyroom.ai.service.AiKbService;
import com.ruoyi.studyroom.ai.service.AiToolExecutor;
import com.ruoyi.studyroom.ai.service.ContentSecurityService;
import com.ruoyi.studyroom.ai.service.LlmClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * AI 对话编排：
 * 1. 会话与历史管理（内存友好，只取最近 N 轮）
 * 2. RAG 检索注入上下文
 * 3. 工具调用循环（最多 2 轮，防止失控）
 * 4. Mock 模式规则兜底（无 Key 可演示）
 * 5. 全程留痕：token 用量/延迟/工具轨迹/知识库命中/用户反馈
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private static final int MAX_TOOL_ROUNDS = 2;

    /** 用户最近一次对话时间戳(用于节流, 防止刷屏触发免费模型限流) */
    private final java.util.concurrent.ConcurrentHashMap<Long, Long> lastChatTs = new java.util.concurrent.ConcurrentHashMap<>();

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;
    private final LlmClient llmClient;
    private final AiToolExecutor toolExecutor;
    private final AiKbService kbService;
    private final StudyroomAiProperties props;
    private final ContentSecurityService contentSecurity;

    /** 输出被判定违规时替换的兑底文案 */
    private static final String RISK_FALLBACK_REPLY =
        "这条回复可能包含不适宜展示的内容，已为你隐藏。换个问题试试吧～";

    @Override
    public ChatReplyVo chat(Long userId, Long sessionId, String message) {
        long start = System.currentTimeMillis();

        // 内容安全：用户输入过检（msgSecCheck 2.0），违规不进模型、不入库
        if (!contentSecurity.checkText(message, currentWxOpenid())) {
            ChatReplyVo vo = new ChatReplyVo();
            vo.setSessionId(sessionId);
            vo.setReply("你的消息包含不适宜的内容，请调整后再发给我哦～");
            vo.setLatencyMs(System.currentTimeMillis() - start);
            vo.setMock(false);
            return vo;
        }

        AiChatSession session = getOrCreateSession(userId, sessionId);
        saveMessage(userId, session.getId(), "user", message, null);

        // 会话标题：取首条消息前 12 字
        if (StrUtil.isBlank(session.getTitle()) || "新对话".equals(session.getTitle())) {
            session.setTitle(StrUtil.sub(message, 0, 12));
            sessionMapper.updateById(session);
        }

        // 节流: 同一用户 10 秒内最多一次模型请求, 防止刷屏/频繁触发免费模型限流
        Long nowMs = System.currentTimeMillis();
        Long lastTs = lastChatTs.putIfAbsent(userId, nowMs);
        if (lastTs != null) {
            long gap = nowMs - lastTs;
            if (gap < 10000) {
                ChatReplyVo vo = new ChatReplyVo();
                vo.setSessionId(session.getId());
                vo.setReply("请稍等一下再问哦，我正在处理上一条问题～（10 秒内只能提问一次）");
                vo.setLatencyMs(1L);
                vo.setMock(false);
                return vo;
            }
            lastChatTs.put(userId, nowMs);
        }

        // 话题判定：与自习室无关的请求(闲聊/娱乐/无关内容)直接拒绝, 不调用模型
        if (!isStudyroomTopic(message)) {
            String refuse = "我是答岸自习室的 AI 学伴，只处理自习室相关的问题哦，比如座位预约、余额查询、卡券使用、营业时间、学习记录等，有什么可以帮你的吗？";
            ChatReplyVo vo = new ChatReplyVo();
            vo.setSessionId(session.getId());
            vo.setReply(refuse);
            vo.setLatencyMs(System.currentTimeMillis() - start);
            vo.setMock(false);
            saveAssistant(userId, session.getId(), refuse, java.util.Collections.emptyList(),
                java.util.Collections.emptyList(), 0, 0, System.currentTimeMillis() - start, "rule");
            return vo;
        }

        // RAG 检索
        List<AiKbService.KbHit> hits = new ArrayList<>();
        if (Boolean.TRUE.equals(props.getKbEnabled())) {
            try {
                hits = kbService.retrieve(message, props.getKbTopK());
            } catch (Exception e) {
                log.warn("[AI] 知识库检索异常，跳过 RAG: {}", e.getMessage());
            }
        }

        // 事实性问题(营业/价格/规则/沉浸区/地址/电话等)且知识库命中: 直接按知识库回答, 不调用模型/工具
        // 避免免费模型工具选择偏差答非所问
        if (hits != null && !hits.isEmpty() && isFactualKbQuestion(message)) {
            StringBuilder kb = new StringBuilder("根据知识库：\n");
            int shown = 0;
            for (AiKbService.KbHit hit : hits) {
                String c = hit.getContent() == null ? "" : hit.getContent().trim();
                if (StrUtil.isNotBlank(c) && shown < 1) {
                    // 截断到 150 字, 保证回答简洁
                    String brief = c.length() > 150 ? c.substring(0, 150) + "…" : c;
                    kb.append(brief).append("\n");
                    shown++;
                }
            }
            String kbReply = kb.toString().trim();
            ChatReplyVo vo = new ChatReplyVo();
            vo.setSessionId(session.getId());
            vo.setReply(kbReply);
            vo.setKbHits(toHitVos(hits));
            vo.setLatencyMs(System.currentTimeMillis() - start);
            vo.setMock(false);
            // 知识库内容为运营录入，仍过一道输出检，保持全链路一致
            if (!contentSecurity.checkText(kbReply, currentWxOpenid())) {
                vo.setReply(RISK_FALLBACK_REPLY);
                kbReply = RISK_FALLBACK_REPLY;
            }
            saveAssistant(userId, session.getId(), kbReply, new ArrayList<>(), hits, 0, 0,
                System.currentTimeMillis() - start, "kb");
            return vo;
        }

        // 组装消息
        List<JSONObject> messages = buildMessages(userId, session.getId(), message, hits);

        // 输出过检已前移至各生成路径入库前（llmChat/mockChat/fallbackReply/KB 直答），
        // 避免出口处重复调用微信接口（msgSecCheck 有 QPS/日调用量限制）
        ChatReplyVo replyVo;
        if (llmClient.isMock()) {
            replyVo = mockChat(userId, session.getId(), message, hits, start);
        } else {
            replyVo = llmChat(userId, session.getId(), messages, hits, start);
        }
        return replyVo;
    }

    /**
     * 获取当前登录微信用户的 openid（用于 msgSecCheck 提升判定准确度）。
     * 会话异常/非小程序登录态时返回 null，不影响主流程。
     */
    private String currentWxOpenid() {
        try {
            XcxLoginUser wxUser = LoginHelper.getWxLoginUser();
            return wxUser == null ? null : wxUser.getOpenid();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 话题判定：判断用户消息是否与自习室业务相关
     * 命中关键词则放行给模型；完全无关的话题(闲聊/娱乐等)直接拒绝, 防止用户拿 AI 随便玩
     */
    private boolean isStudyroomTopic(String message) {
        if (StrUtil.isBlank(message)) {
            return false;
        }
        String msg = message.trim().toLowerCase();
        // 身份/问候类问题(你是谁/你好等)放行, 让模型声明身份
        String[] identity = {"你是谁", "你叫什么", "你是什么", "你好", "您好", "hi", "hello", "在吗", "你能做什么", "介绍一下你", "自我介绍"};
        for (String k : identity) {
            if (msg.contains(k)) return true;
        }
        // 自习室业务关键词
        String[] business = {
            "自习室", "座位", "预约", "订座", "占座", "选座", "余额", "充值", "卡券", "日卡", "周卡", "月卡", "季卡", "年卡",
            "价格", "多少钱", "收费", "费用", "营业", "开门", "关门", "几点", "时间", "学习", "时长", "排名", "排行榜",
            "记录", "门禁", "人脸", "刷脸", "开灯", "关灯", "灯光", "灯", "wifi", "wifi密码", "储物柜", "柜子",
            "门店", "地址", "电话", "在哪", "怎么去", "规则", "政策", "退卡", "退款", "取消", "续费", "套餐", "优惠",
            "帮忙", "帮查", "查一下", "多少钱", "有什么卡", "怎么预约", "怎么用", "哪个座位", "剩几个", "空位"
        };
        for (String k : business) {
            if (msg.contains(k)) return true;
        }
        // 知识库命中也放行(可能问的是知识库里的内容)
        return false;
    }

    /**
     * 判断是否为事实性问题(营业时间/价格/规则/分区/沉浸区/地址/电话等知识库固定信息)
     * 这类问题直接按知识库回答, 避免模型工具选择偏差
     */
    private boolean isFactualKbQuestion(String message) {
        if (StrUtil.isBlank(message)) return false;
        String msg = message.trim().toLowerCase();
        String[] keys = {
            "营业", "开门", "关门", "几点", "时间", "营业时间", "价格", "多少钱", "收费", "费用", "规则", "政策",
            "轻音", "静音", "沉浸", "帘", "分区", "区域", "座位号", "几个座", "地址", "电话", "位置", "在哪",
            "wifi", "密码", "储物柜", "柜子", "门禁", "人脸", "刷脸", "灯光", "开关灯", "怎么预约", "怎么用"
        };
        for (String k : keys) {
            if (msg.contains(k)) return true;
        }
        return false;
    }

    /**
     * 根据工具执行结果生成用户可读的友好回复(当模型未给出有效总结时兜底)
     */
    private String buildToolAnswer(List<ChatReplyVo.ToolTraceVo> toolTrace, List<AiKbService.KbHit> hits) {
        if (toolTrace == null || toolTrace.isEmpty()) {
            // 无工具调用但有知识库命中, 用知识库首条拼一句
            if (hits != null && !hits.isEmpty()) {
                return "根据知识库：\n" + hits.get(0).getContent().trim();
            }
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (ChatReplyVo.ToolTraceVo t : toolTrace) {
            String name = t == null ? "" : String.valueOf(t.getName());
            String resultStr = t == null ? "" : String.valueOf(t.getResult());
            try {
                if (name.contains("balance")) {
                    JSONObject obj = JSONUtil.parseObj(resultStr);
                    sb.append("你的账户余额为 ").append(obj.getInt("balance", 0)).append(" 点。\n");
                    JSONObject cards = obj.getJSONObject("cards");
                    if (cards != null) {
                        sb.append("你拥有的卡券：");
                        boolean first = true;
                        for (String k : cards.keySet()) {
                            if (!first) sb.append("、");
                            sb.append(k);
                            first = false;
                        }
                        sb.append("。\n");
                    }
                } else if (name.contains("seats")) {
                    JSONObject obj = JSONUtil.parseObj(resultStr);
                    int total = obj.getInt("totalSeats", 0);
                    int occupied = obj.getInt("occupiedCount", 0);
                    int available = obj.getInt("availableCount", total - occupied);
                    int vip = obj.getInt("vipSeats", 0);
                    int common = obj.getInt("commonSeats", 0);
                    sb.append("当前共有 ").append(total).append(" 个座位，其中已预约 ").append(occupied)
                        .append(" 个，空闲 ").append(Math.max(0, available)).append(" 个。\n");
                    if (vip > 0 && common > 0) {
                        sb.append("（轻音室 ").append(common).append(" 个 / 静音室 ").append(vip).append(" 个）\n");
                    }
                    Object list = obj.get("availableSeats");
                    if (list != null && list.toString().length() > 2) {
                        sb.append("空闲座位号：").append(list.toString()).append("\n");
                    }
                } else if (name.contains("learn")) {
                    JSONObject obj = JSONUtil.parseObj(resultStr);
                    sb.append("你的累计学习时长为 ").append(obj.getStr("hours", "0")).append(" 小时。\n");
                    if (obj.containsKey("rank")) {
                        sb.append("当前排名第 ").append(obj.getInt("rank", 0)).append(" 名。\n");
                    }
                } else if (name.contains("store")) {
                    JSONObject obj = JSONUtil.parseObj(resultStr);
                    // 工具返回 {store1:{name,address,phone}, ...}
                    JSONObject s1 = obj.getJSONObject("store1");
                    if (s1 != null) {
                        sb.append("答岸自习室门店信息：\n");
                        sb.append("店名：").append(StrUtil.blankToDefault(s1.getStr("name"), "答岸自习室")).append("\n");
                        if (StrUtil.isNotBlank(s1.getStr("address"))) {
                            sb.append("地址：").append(s1.getStr("address")).append("\n");
                        }
                        if (StrUtil.isNotBlank(s1.getStr("phone"))) {
                            sb.append("电话：").append(s1.getStr("phone")).append("（微信同号）\n");
                        }
                    } else {
                        sb.append("答岸自习室门店信息：").append(obj.getStr("roomName", "答岸自习室")).append("\n");
                        if (obj.containsKey("phone")) sb.append("电话：").append(obj.getStr("phone")).append("\n");
                        if (obj.containsKey("addr")) sb.append("地址：").append(obj.getStr("addr")).append("\n");
                    }
                } else {
                    sb.append(resultStr).append("\n");
                }
            } catch (Exception e) {
                sb.append(resultStr).append("\n");
            }
        }
        String out = sb.toString().trim();
        if (StrUtil.isNotBlank(out)) {
            return out;
        }
        if (hits != null && !hits.isEmpty()) {
            return "根据知识库：\n" + hits.get(0).getContent().trim();
        }
        return "";
    }

    // ---------- 真实模型对话 ----------

    private ChatReplyVo llmChat(Long userId, Long sessionId, List<JSONObject> messages,
                                List<AiKbService.KbHit> hits, long start) {
        String toolsJson = toolExecutor.toolDefinitionsJson();
        LlmClient.LlmChatResult result;
        // 模型调用失败时自动重试一次(免费模型偶发 429 限流), 仍失败则降级为知识库回答
        try {
            result = llmClient.chat(messages, toolsJson);
        } catch (Exception e1) {
            log.warn("[AI] 模型调用失败，2s 后重试: {}", e1.getMessage());
            try {
                Thread.sleep(2000);
                result = llmClient.chat(messages, toolsJson);
            } catch (Exception e2) {
                log.error("[AI] 模型调用重试仍失败，降级为知识库回答", e2);
                // 降级: 用知识库内容组装回答, 保证用户有可用回复
                String degraded = buildToolAnswer(new ArrayList<>(), hits);
                if (StrUtil.isBlank(degraded)) {
                    degraded = "抱歉，AI 服务暂时不可用，请稍后再试。你也可以先让我帮你查余额、座位或学习时长～";
                }
                return fallbackReply(userId, sessionId, hits, start, degraded);
            }
        }

        int totalPrompt = 0;
        int totalCompletion = 0;
        List<ChatReplyVo.ToolTraceVo> toolTrace = new ArrayList<>();
        int round = 0;

        while (result.getToolCalls() != null && !result.getToolCalls().isEmpty() && round < MAX_TOOL_ROUNDS) {
            totalPrompt += result.getPromptTokens();
            totalCompletion += result.getCompletionTokens();

            JSONObject assistantMsg = JSONUtil.createObj()
                .set("role", "assistant")
                .set("content", result.getContent() == null ? "" : result.getContent());
            JSONArray toolCallsArray = new JSONArray();
            for (LlmClient.ToolCall call : result.getToolCalls()) {
                if (!toolExecutor.isBuiltin(call.getName())) {
                    continue;
                }
                String out;
                try {
                    out = toolExecutor.execute(call.getName(), call.getArguments());
                } catch (Exception e) {
                    out = JSONUtil.createObj().set("error", e.getMessage()).toString();
                }
                toolCallsArray.add(JSONUtil.createObj()
                    .set("id", call.getId())
                    .set("type", "function")
                    .set("function", JSONUtil.createObj()
                        .set("name", call.getName())
                        .set("arguments", call.getArguments())));
                messages.add(JSONUtil.createObj()
                    .set("role", "tool")
                    .set("tool_call_id", call.getId())
                    .set("content", out));
                toolTrace.add(buildTrace(call.getName(), call.getArguments(), out));
            }
            if (!toolCallsArray.isEmpty()) {
                assistantMsg.set("tool_calls", toolCallsArray);
                messages.add(assistantMsg);
            }
            try {
                result = llmClient.chat(messages, toolsJson);
            } catch (Exception e) {
                log.error("[AI] 工具轮模型调用失败", e);
                break;
            }
            round++;
        }

        totalPrompt += result.getPromptTokens();
        totalCompletion += result.getCompletionTokens();
        String modelContent = result.getContent() == null ? "" : result.getContent().trim();
        // 若模型未给出有效总结(空/套话), 用工具结果直接生成用户可读回复
        String finalContent = modelContent;
        if (StrUtil.isBlank(finalContent)
            || finalContent.contains("已经帮你处理好了")
            || finalContent.contains("还有其他需要吗")
            || finalContent.contains("已为您处理")) {
            String built = buildToolAnswer(toolTrace, hits);
            finalContent = StrUtil.isNotBlank(built) ? built : "我已经帮你处理好了，还有其他需要吗？";
        }
        long latency = System.currentTimeMillis() - start;

        ChatReplyVo vo = new ChatReplyVo();
        vo.setSessionId(sessionId);
        vo.setReply(finalContent);
        vo.setToolTrace(toolTrace);
        vo.setKbHits(toHitVos(hits));
        vo.setPromptTokens(totalPrompt);
        vo.setCompletionTokens(totalCompletion);
        vo.setModel(llmClient.isMock() ? "mock" : props.getChatModel());
        vo.setLatencyMs(latency);
        vo.setMock(llmClient.isMock());

        // 输出过检（AIGC 合规）：违规替换为兜底文案后再入库
        if (!contentSecurity.checkText(finalContent, currentWxOpenid())) {
            finalContent = RISK_FALLBACK_REPLY;
            vo.setReply(finalContent);
            toolTrace.clear();
        }
        saveAssistant(userId, sessionId, finalContent, toolTrace, hits, totalPrompt, totalCompletion, latency, result.getModel());
        return vo;
    }

    // ---------- Mock 模式（规则兜底） ----------

    private ChatReplyVo mockChat(Long userId, Long sessionId, String message,
                                 List<AiKbService.KbHit> hits, long start) {
        String toolName = null;
        if (containsAny(message, "多少钱", "价格", "收费") && !hits.isEmpty()) {
            // 价格类问题优先走知识库
        } else if (containsAny(message, "余额", "充钱", "充值", "账户")) {
            toolName = "query_balance";
        } else if (containsAny(message, "座位", "空座", "有没有位置", "订座", "预约", "座位吗")) {
            toolName = "query_available_seats";
        } else if (containsAny(message, "学习", "时长", "排名", "记录", "今天学")) {
            toolName = "query_learn_hours";
        } else if (containsAny(message, "门店", "地址", "电话", "在哪", "营业")) {
            toolName = "query_store_info";
        }
        String reply;
        List<ChatReplyVo.ToolTraceVo> toolTrace = new ArrayList<>();
        if (toolName != null) {
            String out = toolExecutor.execute(toolName, "{}");
            toolTrace.add(buildTrace(toolName, "{}", out));
            reply = mockAnswer(toolName, out);
        } else if (!hits.isEmpty()) {
            StringBuilder sb = new StringBuilder("根据自习室知识库：\n");
            int i = 1;
            for (AiKbService.KbHit hit : hits) {
                sb.append(i).append(". ").append(hit.getContent().trim()).append("\n");
                i++;
            }
            reply = sb.toString();
        } else {
            reply = "我是「米娅桑自习室」的 AI 学伴小柚～目前处于离线演示模式（未配置 API Key）。"
                + "你可以问我：查余额、查座位、学习时长/排名、门店信息；也可以往知识库上传学习资料后问我相关内容。";
        }
        long latency = System.currentTimeMillis() - start;
        ChatReplyVo vo = new ChatReplyVo();
        vo.setSessionId(sessionId);
        vo.setReply(reply);
        vo.setToolTrace(toolTrace);
        vo.setKbHits(toHitVos(hits));
        vo.setPromptTokens(0);
        vo.setCompletionTokens(0);
        vo.setModel("mock");
        vo.setLatencyMs(latency);
        vo.setMock(true);
        // 输出过检：违规替换为兑底文案后再入库
        if (!contentSecurity.checkText(reply, currentWxOpenid())) {
            reply = RISK_FALLBACK_REPLY;
            vo.setReply(reply);
            toolTrace.clear();
        }
        saveAssistant(userId, sessionId, reply, toolTrace, hits, 0, 0, latency, "mock");
        return vo;
    }

    private ChatReplyVo fallbackReply(Long userId, Long sessionId, List<AiKbService.KbHit> hits,
                                      long start, String reply) {
        long latency = System.currentTimeMillis() - start;
        ChatReplyVo vo = new ChatReplyVo();
        vo.setSessionId(sessionId);
        vo.setReply(reply);
        vo.setKbHits(toHitVos(hits));
        vo.setModel("fallback");
        vo.setLatencyMs(latency);
        vo.setMock(true);
        // 输出过检：违规替换为兑底文案后再入库
        if (!contentSecurity.checkText(reply, currentWxOpenid())) {
            reply = RISK_FALLBACK_REPLY;
            vo.setReply(reply);
        }
        saveAssistant(userId, sessionId, reply, new ArrayList<>(), hits, 0, 0, latency, "fallback");
        return vo;
    }

    private String mockAnswer(String toolName, String resultJson) {
        JSONObject r = JSONUtil.parseObj(resultJson);
        switch (toolName) {
            case "query_balance":
                return "你的账户余额是 " + r.getLong("balance", 0L) + " 元，持有的优惠卡：" + describeCards(r.getJSONObject("cards"));
            case "query_available_seats":
                return "当前可用座位 " + r.getInt("availableCount", 0) + " 个（静音室 " + r.getInt("vipSeats", 0)
                    + " 个 / 轻音室 " + r.getInt("commonSeats", 0) + " 个），已占用 " + r.getInt("occupiedCount", 0) + " 个。"
                    + (r.getInt("availableCount", 0) > 0 ? "需要的话可以直接去预约～" : "暂时满座，建议换个时间段再来。");
            case "query_learn_hours":
                return "你累计学习了 " + r.getStr("hours") + " 小时，当前排名第 " + r.getInt("rank", 0)
                    + " 名（共 " + r.getInt("totalUsers", 0) + " 人参与）";
            case "query_store_info":
                return describeStores(r);
            default:
                return resultJson;
        }
    }

    private String describeCards(JSONObject cards) {
        if (cards == null || cards.isEmpty()) {
            return "无";
        }
        List<String> parts = new ArrayList<>();
        cards.forEach((k, v) -> parts.add(k + "(到期 " + v + ")"));
        return String.join("、", parts);
    }

    private String describeStores(JSONObject r) {
        StringBuilder sb = new StringBuilder();
        r.forEach((k, v) -> {
            JSONObject s = (JSONObject) v;
            sb.append("【").append(s.getStr("name")).append("】")
                .append("地址：").append(s.getStr("address"))
                .append("，电话：").append(s.getStr("phone")).append("；");
        });
        return sb.length() == 0 ? "暂未录入门店信息" : sb.toString();
    }

    // ---------- 消息与历史 ----------

    private List<JSONObject> buildMessages(Long userId, Long sessionId, String message, List<AiKbService.KbHit> hits) {
        List<JSONObject> messages = new ArrayList<>();
        messages.add(JSONUtil.createObj().set("role", "system").set("content", systemPrompt(hits)));
        // 历史：最近 N 轮（user/assistant）
        int limit = props.getHistoryTurns() * 2;
        List<AiChatMessage> history = messageMapper.selectList(
            new LambdaQueryWrapper<AiChatMessage>()
                .eq(AiChatMessage::getSessionId, sessionId)
                .in(AiChatMessage::getRole, "user", "assistant")
                .orderByDesc(AiChatMessage::getId)
                .last("LIMIT " + limit));
        for (int i = history.size() - 1; i >= 0; i--) {
            AiChatMessage m = history.get(i);
            messages.add(JSONUtil.createObj().set("role", m.getRole()).set("content", StrUtil.blankToDefault(m.getContent(), "")));
        }
        messages.add(JSONUtil.createObj().set("role", "user").set("content", message));
        return messages;
    }

    private String systemPrompt(List<AiKbService.KbHit> hits) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是「答岸自习室」的官方 AI 学伴，用户都是来答岸自习室学习的学生。").append("\n");
        sb.append("规则1：用户问你是谁时，回答\"我是答岸自习室的 AI 学伴，负责座位预约、余额、卡券、学习记录、营业时间、价格等自习室业务，有需要随时问我\"。").append("\n");
        sb.append("规则2：用户问的都是自习室相关问题（营业时间、价格、座位、余额、卡券、规则、门禁、灯光、门店信息），必须认真回答。营业时间/价格/规则等用知识库内容回答；余额/座位/学习时长等实时数据必须调用工具，以工具结果为准，不编造数字。").append("\n");
        sb.append("规则3：回答简洁自然，口语化，60-120字，先给结论再给建议。").append("\n");
        sb.append("工具：query_balance、query_available_seats、query_learn_hours、query_store_info。").append("\n");
        if (hits != null && !hits.isEmpty()) {
            sb.append("\n【知识库】\n");
            int i = 1;
            for (AiKbService.KbHit hit : hits) {
                sb.append("[").append(i).append("] 《").append(hit.getDocumentTitle()).append("》")
                    .append(hit.getContent().trim()).append("\n");
                i++;
            }
        }
        return sb.toString();
    }

    private AiChatSession getOrCreateSession(Long userId, Long sessionId) {
        if (sessionId != null) {
            AiChatSession session = sessionMapper.selectById(sessionId);
            if (session != null) {
                return session;
            }
        }
        AiChatSession session = new AiChatSession();
        session.setUserId(userId);
        session.setTitle("新对话");
        sessionMapper.insert(session);
        return session;
    }

    private void saveMessage(Long userId, Long sessionId, String role, String content, String toolResultsJson) {
        AiChatMessage m = new AiChatMessage();
        m.setUserId(userId);
        m.setSessionId(sessionId);
        m.setRole(role);
        m.setContent(content);
        m.setToolResultsJson(toolResultsJson);
        messageMapper.insert(m);
    }

    private void saveAssistant(Long userId, Long sessionId, String content, List<ChatReplyVo.ToolTraceVo> toolTrace,
                               List<AiKbService.KbHit> hits, int prompt, int completion, long latency, String model) {
        AiChatMessage m = new AiChatMessage();
        m.setUserId(userId);
        m.setSessionId(sessionId);
        m.setRole("assistant");
        m.setContent(content);
        m.setToolCallsJson(toolTrace.isEmpty() ? null : JSONUtil.toJsonStr(toolTrace));
        m.setPromptTokens(prompt);
        m.setCompletionTokens(completion);
        m.setLatencyMs((int) latency);
        m.setModel(model);
        m.setKbHitsJson(hits.isEmpty() ? null : JSONUtil.toJsonStr(toHitVos(hits)));
        messageMapper.insert(m);
    }

    private ChatReplyVo.ToolTraceVo buildTrace(String name, String arguments, String result) {
        ChatReplyVo.ToolTraceVo trace = new ChatReplyVo.ToolTraceVo();
        trace.setName(name);
        trace.setArguments(arguments);
        trace.setResult(result);
        return trace;
    }

    private List<ChatReplyVo.KbHitVo> toHitVos(List<AiKbService.KbHit> hits) {
        List<ChatReplyVo.KbHitVo> vos = new ArrayList<>();
        if (hits != null) {
            for (AiKbService.KbHit hit : hits) {
                ChatReplyVo.KbHitVo vo = new ChatReplyVo.KbHitVo();
                vo.setTitle(hit.getDocumentTitle());
                vo.setContent(hit.getContent());
                vo.setScore(hit.getScore());
                vos.add(vo);
            }
        }
        return vos;
    }

    private boolean containsAny(String text, String... keys) {
        for (String key : keys) {
            if (StrUtil.containsIgnoreCase(text, key)) {
                return true;
            }
        }
        return false;
    }

    // ---------- 会话 / 反馈 / 统计 ----------

    @Override
    public List<AiChatSession> listSessions(Long userId) {
        return sessionMapper.selectList(new LambdaQueryWrapper<AiChatSession>()
            .eq(AiChatSession::getUserId, userId)
            .orderByDesc(AiChatSession::getUpdateTime));
    }

    @Override
    public List<Map<String, Object>> listMessages(Long userId, Long sessionId) {
        List<AiChatMessage> list = messageMapper.selectList(new LambdaQueryWrapper<AiChatMessage>()
            .eq(AiChatMessage::getSessionId, sessionId)
            .eq(AiChatMessage::getUserId, userId)
            .orderByAsc(AiChatMessage::getId));
        List<Map<String, Object>> result = new ArrayList<>();
        for (AiChatMessage m : list) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", m.getId());
            item.put("role", m.getRole());
            item.put("content", m.getContent());
            item.put("rating", m.getRating());
            item.put("toolCalls", m.getToolCallsJson() == null ? null : JSONUtil.parse(m.getToolCallsJson()));
            item.put("kbHits", m.getKbHitsJson() == null ? null : JSONUtil.parse(m.getKbHitsJson()));
            item.put("model", m.getModel());
            item.put("createTime", m.getCreateTime());
            result.add(item);
        }
        return result;
    }

    @Override
    public void feedback(Long userId, Long messageId, Integer rating, String comment) {
        AiChatMessage m = messageMapper.selectById(messageId);
        if (m == null || !m.getUserId().equals(userId)) {
            throw new IllegalArgumentException("消息不存在或无权操作");
        }
        m.setRating(rating);
        m.setRatingComment(comment);
        messageMapper.updateById(m);
    }

    @Override
    public Map<String, Object> stats(Long userId) {
        Long msgCount = messageMapper.selectCount(new LambdaQueryWrapper<AiChatMessage>()
            .eq(AiChatMessage::getUserId, userId));
        Long sessionCount = sessionMapper.selectCount(new LambdaQueryWrapper<AiChatSession>()
            .eq(AiChatSession::getUserId, userId));
        Long likeCount = messageMapper.selectCount(new LambdaQueryWrapper<AiChatMessage>()
            .eq(AiChatMessage::getUserId, userId).eq(AiChatMessage::getRating, 1));
        Long dislikeCount = messageMapper.selectCount(new LambdaQueryWrapper<AiChatMessage>()
            .eq(AiChatMessage::getUserId, userId).eq(AiChatMessage::getRating, -1));
        Long toolCount = messageMapper.selectCount(new LambdaQueryWrapper<AiChatMessage>()
            .eq(AiChatMessage::getUserId, userId).isNotNull(AiChatMessage::getToolCallsJson));
        Map<String, Object> map = new HashMap<>();
        map.put("messageCount", msgCount);
        map.put("sessionCount", sessionCount);
        map.put("likeCount", likeCount);
        map.put("dislikeCount", dislikeCount);
        map.put("toolCallCount", toolCount);
        map.put("mockMode", llmClient.isMock());
        map.put("chatModel", props.getChatModel());
        return map;
    }

}