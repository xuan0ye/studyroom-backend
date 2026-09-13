package com.ruoyi.studyroom.service;

import com.ruoyi.RuoYiApplication;
import com.ruoyi.common.core.domain.model.LoginUser;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.studyroom.ai.config.StudyroomAiProperties;
import com.ruoyi.studyroom.ai.domain.vo.ChatReplyVo;
import com.ruoyi.studyroom.ai.service.AiChatService;
import com.ruoyi.studyroom.ai.service.AiKbService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * AI 学伴冒烟测试（Mock 模式，无需 API Key）
 * 验证：知识库入库/检索、Mock 对话工具调用、知识库回答、反馈留痕
 *
 * 说明：通过反射注入 LoginHelper 的 ThreadLocal 登录态，
 * 避免依赖 Sa-Token 会话与 Servlet 请求上下文，便于在纯 JUnit 环境验证业务链路。
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = RuoYiApplication.class)
public class AiSmokeTest {

    /** 本地 mock 登录稳定用户 */
    private static final Long TEST_USER_ID = 2083443341764120577L;

    @Autowired
    private AiChatService chatService;
    @Autowired
    private AiKbService kbService;
    @Autowired
    private StudyroomAiProperties aiProps;

    @Before
    public void setUp() throws Exception {
        LoginUser user = new LoginUser();
        user.setUserId(TEST_USER_ID);
        user.setUsername("ai-smoke-test");
        loginCache().set(user);
        // 强制 Mock 模式
        aiProps.setApiKey("");
        aiProps.setEnabled(true);
        aiProps.setKbEnabled(true);
    }

    @After
    public void tearDown() throws Exception {
        loginCache().remove();
        LoginHelper.clearCache();
    }

    @SuppressWarnings("unchecked")
    private ThreadLocal<LoginUser> loginCache() throws Exception {
        Field f = LoginHelper.class.getDeclaredField("LOGIN_CACHE");
        f.setAccessible(true);
        return (ThreadLocal<LoginUser>) f.get(null);
    }

    @Test
    public void testKbAddAndRetrieve() {
        kbService.addDocument("自习室规则-冒烟测试",
            "米娅桑自习室营业时间为每天 8:00-22:30。包月卡价格为 299 元/月，可无限次预约座位。");
        List<AiKbService.KbHit> hits = kbService.retrieve("营业时间是几点", 2);
        assertFalse("知识库应能召回分片", hits.isEmpty());
        String top = hits.get(0).getContent();
        assertTrue("召回内容应相关", top.contains("营业时间") || top.contains("8:00"));
    }

    @Test
    public void testMockChatBalanceTool() {
        ChatReplyVo vo = chatService.chat(TEST_USER_ID, null, "帮我查一下账户余额");
        assertNotNull(vo.getSessionId());
        assertTrue("应调用 query_balance 工具", vo.getToolTrace().stream()
            .anyMatch(t -> "query_balance".equals(t.getName())));
        assertTrue("回复应包含余额", vo.getReply().contains("余额"));
        assertTrue("应为 Mock 模式", Boolean.TRUE.equals(vo.getMock()));
    }

    @Test
    public void testMockChatSeatsTool() {
        ChatReplyVo vo = chatService.chat(TEST_USER_ID, null, "现在还有空座位吗");
        assertTrue("应调用座位工具", vo.getToolTrace().stream()
            .anyMatch(t -> "query_available_seats".equals(t.getName())));
    }

    @Test
    public void testMockChatKbAnswer() {
        kbService.addDocument("价目表-冒烟测试", "自习室单次卡 10 元/次，日卡 29 元，月卡 299 元，季卡 799 元。");
        ChatReplyVo vo = chatService.chat(TEST_USER_ID, null, "日卡多少钱？");
        assertTrue("未命中工具时应回答知识库内容", vo.getReply().contains("日卡"));
    }

    @Test
    public void testFeedbackLoop() {
        ChatReplyVo vo = chatService.chat(TEST_USER_ID, null, "帮我查一下余额");
        List<Map<String, Object>> messages = chatService.listMessages(TEST_USER_ID, vo.getSessionId());
        Map<String, Object> last = messages.get(messages.size() - 1);
        assertEquals("最后一条应为 AI 回复", "assistant", last.get("role"));
        Long messageId = (Long) last.get("id");
        assertNotNull(messageId);
        chatService.feedback(TEST_USER_ID, messageId, 1, "回答准确");
        List<Map<String, Object>> after = chatService.listMessages(TEST_USER_ID, vo.getSessionId());
        assertEquals("反馈应落库", 1, after.get(after.size() - 1).get("rating"));
    }

}