package com.ruoyi.studyroom.wx.api;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.studyroom.ai.domain.AiChatSession;
import com.ruoyi.studyroom.ai.domain.AiKbDocument;
import com.ruoyi.studyroom.ai.domain.vo.ChatReplyVo;
import com.ruoyi.studyroom.ai.service.AiChatService;
import com.ruoyi.studyroom.ai.service.AiKbService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * AI 学伴接口
 * 覆盖：对话（RAG + 工具调用）、会话管理、用户反馈（数据飞轮）、知识库管理、统计
 */
@Slf4j
@Api(value = "AI 学伴", tags = {"AI 学伴"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/wx/ai")
public class AiController {

    private final AiChatService chatService;
    private final AiKbService kbService;

    @ApiOperation("发起一轮对话")
    @PostMapping("/chat")
    public R<ChatReplyVo> chat(@RequestBody JSONObject body) {
        String message = body.getStr("message");
        Long sessionId = body.getLong("sessionId");
        if (StrUtil.isBlank(message)) {
            return R.fail("消息内容不能为空");
        }
        try {
            return R.ok(chatService.chat(LoginHelper.getUserId(), sessionId, message.trim()));
        } catch (Exception e) {
            log.error("[AI] chat 异常", e);
            return R.fail("AI 服务异常：" + e.getMessage());
        }
    }

    @ApiOperation("会话列表")
    @GetMapping("/sessions")
    public R<List<AiChatSession>> sessions() {
        return R.ok(chatService.listSessions(LoginHelper.getUserId()));
    }

    @ApiOperation("会话消息列表")
    @GetMapping("/sessions/{sessionId}/messages")
    public R<List<Map<String, Object>>> messages(@PathVariable Long sessionId) {
        return R.ok(chatService.listMessages(LoginHelper.getUserId(), sessionId));
    }

    @ApiOperation("提交反馈（赞/踩+备注），回流数据飞轮")
    @PostMapping("/feedback")
    public R<Void> feedback(@RequestBody JSONObject body) {
        Long messageId = body.getLong("messageId");
        Integer rating = body.getInt("rating");
        if (messageId == null || rating == null) {
            return R.fail("messageId 与 rating 不能为空");
        }
        try {
            chatService.feedback(LoginHelper.getUserId(), messageId, rating, body.getStr("comment"));
            return R.ok();
        } catch (IllegalArgumentException e) {
            return R.fail(e.getMessage());
        }
    }

    @ApiOperation("AI 使用统计")
    @GetMapping("/stats")
    public R<Map<String, Object>> stats() {
        return R.ok(chatService.stats(LoginHelper.getUserId()));
    }

    @ApiOperation("知识库文档列表")
    @GetMapping("/kb/documents")
    public R<List<AiKbDocument>> documents() {
        if (!LoginHelper.isAdmin()) {
            return R.fail("无权访问");
        }
        return R.ok(kbService.listDocuments());
    }

    @ApiOperation("知识库新增文档（录入后自动切分+向量化）")
    @PostMapping("/kb/documents")
    public R<JSONObject> addDocument(@RequestBody JSONObject body) {
        if (!LoginHelper.isAdmin()) {
            return R.fail("无权操作");
        }
        String title = body.getStr("title");
        String content = body.getStr("content");
        if (StrUtil.isBlank(title) || StrUtil.isBlank(content)) {
            return R.fail("标题与内容不能为空");
        }
        try {
            int chunks = kbService.addDocument(title.trim(), content.trim());
            JSONObject result = new JSONObject();
            result.set("chunkCount", chunks);
            return R.ok(result);
        } catch (Exception e) {
            log.error("[AI] 知识库入库异常", e);
            return R.fail("知识库入库失败：" + e.getMessage());
        }
    }

    @ApiOperation("知识库删除文档")
    @DeleteMapping("/kb/documents/{id}")
    public R<Void> deleteDocument(@PathVariable Long id) {
        if (!LoginHelper.isAdmin()) {
            return R.fail("无权操作");
        }
        kbService.deleteDocument(id);
        return R.ok();
    }

    @ApiOperation("知识库检索调试（查看命中分片与分数）")
    @GetMapping("/kb/search")
    public R<List<AiKbService.KbHit>> search(@RequestParam String query, @RequestParam(defaultValue = "4") Integer topK) {
        if (!LoginHelper.isAdmin()) {
            return R.fail("无权访问");
        }
        return R.ok(kbService.retrieve(query, topK));
    }

}