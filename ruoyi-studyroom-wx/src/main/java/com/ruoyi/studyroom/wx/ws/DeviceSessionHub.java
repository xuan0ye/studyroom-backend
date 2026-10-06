package com.ruoyi.studyroom.wx.ws;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.studyroom.domain.AccessDevice;
import com.ruoyi.studyroom.mapper.AccessDeviceMapper;
import com.ruoyi.studyroom.service.IAccessControlService;
import com.ruoyi.studyroom.service.IDeviceChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.websocket.Session;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 门禁设备长连接会话中枢(WebSocket)。
 *
 * <p>设备按商家文档主动连到本系统, 之后所有下行命令都走这条连接并等设备回执,
 * 因此这里是"下发是否真的送达"的唯一权威来源 —— 不再用 Redis 广播那种发出去就无法确认的方式。</p>
 *
 * <p>JSR-356 的端点实例由容器按连接创建, 拿不到 Spring 注入, 故用静态 INSTANCE 桥接。</p>
 *
 * @author studyroom
 */
@Slf4j
@Component
public class DeviceSessionHub implements IDeviceChannel {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {};

    /** 心跳间隔 15 秒(商家文档), 超过 6 个周期没动静即视为掉线 */
    private static final long ONLINE_WINDOW_MS = 90_000L;

    /** 默认等待设备回执的超时 */
    public static final long DEFAULT_ACK_TIMEOUT_MS = 5_000L;

    private static volatile DeviceSessionHub INSTANCE;

    /** sn -> session */
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    /** session -> sn (用于断线清理) */
    private final Map<Session, String> sessionSn = new ConcurrentHashMap<>();
    /** sn -> 最后活跃毫秒 */
    private final Map<String, Long> lastSeen = new ConcurrentHashMap<>();
    /** 关联键 -> 等待中的回执 */
    private final Map<String, CompletableFuture<Map<String, Object>>> pending = new ConcurrentHashMap<>();

    private final ObjectProvider<IAccessControlService> accessServiceProvider;
    private final ObjectProvider<AccessDeviceMapper> deviceMapperProvider;

    public DeviceSessionHub(ObjectProvider<IAccessControlService> accessServiceProvider,
                            ObjectProvider<AccessDeviceMapper> deviceMapperProvider) {
        this.accessServiceProvider = accessServiceProvider;
        this.deviceMapperProvider = deviceMapperProvider;
    }

    @PostConstruct
    public void init() {
        INSTANCE = this;
        log.info("[门禁WS] DeviceSessionHub 就绪");
    }

    public static DeviceSessionHub get() {
        return INSTANCE;
    }

    // ================= 连接生命周期 =================

    public void onOpen(Session session) {
        log.info("[门禁WS] 连接建立 sessionId={} remote={} query={}",
            session.getId(), session.getRequestURI(), session.getQueryString());
        // sn 可能来自 URL 查询参数(?sn=xxx), 也可能要等第一条报文才知道
        String sn = queryParam(session, "sn");
        if (StringUtils.isNotBlank(sn)) {
            attach(sn, session);
        }
    }

    public void attach(String sn, Session session) {
        sessions.put(sn, session);
        sessionSn.put(session, sn);
        lastSeen.put(sn, System.currentTimeMillis());
        log.info("[门禁WS] 设备已登记 sn={} sessionId={}", sn, session.getId());
        markDeviceOnline(sn);
    }

    /**
     * 设备接上来就刷新 sr_access_device 的 status / last_online_time。
     * 否则后台看到的仍是历史值(此前一直停在 2026-08-16, 与实际严重脱节)。
     */
    private void markDeviceOnline(String sn) {
        try {
            AccessDeviceMapper mapper = deviceMapperProvider.getIfAvailable();
            if (mapper == null) {
                return;
            }
            AccessDevice exist = mapper.selectOne(Wrappers.<AccessDevice>lambdaQuery()
                .eq(AccessDevice::getSn, sn).last("limit 1"));
            if (exist == null) {
                log.warn("[门禁WS] 收到未登记设备接入 sn={}, sr_access_device 中无该序列号", sn);
                return;
            }
            AccessDevice upd = new AccessDevice();
            upd.setId(exist.getId());
            upd.setStatus(1);
            upd.setLastOnlineTime(new Date());
            upd.setUpdateTime(new Date());
            mapper.updateById(upd);
        } catch (Exception e) {
            log.warn("[门禁WS] 更新设备在线状态失败 sn={}: {}", sn, e.getMessage());
        }
    }

    public void onClose(Session session) {
        String sn = sessionSn.remove(session);
        if (sn != null) {
            sessions.remove(sn, session);
        }
        log.warn("[门禁WS] 连接关闭 sn={} sessionId={}", sn, session.getId());
    }

    public void onError(Session session, Throwable t) {
        String sn = sessionSn.get(session);
        log.warn("[门禁WS] 连接异常 sn={} sessionId={}: {}", sn, session.getId(), t == null ? "?" : t.getMessage());
    }

    /** 刷新活跃时间(心跳/任意上报都会调用) */
    public void touch(String sn) {
        if (StringUtils.isNotBlank(sn)) {
            lastSeen.put(sn, System.currentTimeMillis());
        }
    }

    // ================= 入站报文 =================

    public void onMessage(Session session, String text) {
        Map<String, Object> msg;
        try {
            msg = MAPPER.readValue(text, MAP_TYPE);
        } catch (Exception e) {
            log.warn("[门禁WS] 报文非 JSON, 已忽略: {}", abbreviate(text));
            return;
        }
        String cmd = str(msg.get("cmd"));
        String sn = str(msg.get("sn"));
        if (StringUtils.isBlank(sn)) {
            sn = sessionSn.get(session);
        }
        if (StringUtils.isNotBlank(sn)) {
            touch(sn);
            if (!sessionSn.containsKey(session)) {
                attach(sn, session);
            }
        }
        log.info("[门禁WS] 收到 sn={} cmd={} len={}", sn, cmd, text == null ? 0 : text.length());

        // 1) 心跳: 按文档回执服务器时间
        if (cmd != null && cmd.equalsIgnoreCase("F1heartbeat")) {
            Map<String, Object> reply = new HashMap<>();
            reply.put("cmd", "F1heartbeat");
            reply.put("timestamp", System.currentTimeMillis() / 1000);
            reply.put("result", 0);
            reply.put("errmsg", "");
            sendRaw(session, reply);
            return;
        }

        // 2) 命令回执(设备带 result 返回) => 唤醒等待中的下发
        if (msg.containsKey("result")) {
            CompletableFuture<Map<String, Object>> f = null;
            String token = str(msg.get("token"));
            if (StringUtils.isNotBlank(token)) {
                f = pending.get(key(sn, token));
            }
            if (f == null && StringUtils.isNotBlank(cmd)) {
                f = pending.get(key(sn, "#" + cmd));
            }
            if (f != null) {
                f.complete(msg);
                return;
            }
            log.info("[门禁WS] 无匹配回执(可能已超时) sn={} cmd={} result={}", sn, cmd, msg.get("result"));
            return;
        }

        // 3) 其它为设备主动上报(开门记录/人员/陌生人/高温等): 先回执, 再交业务处理
        Map<String, Object> ack = new HashMap<>();
        ack.put("cmd", cmd == null ? "upRecord" : cmd);
        ack.put("result", 0);
        ack.put("errmsg", "");
        sendRaw(session, ack);
        try {
            IAccessControlService svc = accessServiceProvider.getIfAvailable();
            if (svc != null) {
                svc.handleMqttDataReport(msg);
            }
        } catch (Exception e) {
            log.warn("[门禁WS] 上报业务处理失败 cmd={}: {}", cmd, e.getMessage());
        }
    }

    // ================= 下行命令 =================

    @Override
    public Map<String, Object> sendAndAwait(String sn, Map<String, Object> payload, long timeoutMs) {
        if (StringUtils.isBlank(sn) || payload == null) {
            return null;
        }
        Session session = sessions.get(sn);
        if (session == null || !session.isOpen()) {
            log.warn("[门禁WS] 设备不在线, 无法下发 sn={} cmd={}", sn, payload.get("cmd"));
            return null;
        }
        String token = str(payload.get("token"));
        if (StringUtils.isBlank(token)) {
            token = UUID.randomUUID().toString().replace("-", "");
            payload.put("token", token);
        }
        String cmd = str(payload.get("cmd"));
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pending.put(key(sn, token), future);
        // 兜底: 设备若没回 token, 用 sn+cmd 也能对上
        if (StringUtils.isNotBlank(cmd)) {
            pending.put(key(sn, "#" + cmd), future);
        }
        long wait = timeoutMs > 0 ? timeoutMs : DEFAULT_ACK_TIMEOUT_MS;
        try {
            if (!sendRaw(session, payload)) {
                return null;
            }
            return future.get(wait, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("[门禁WS] 等待设备回执超时({}ms) sn={} cmd={}", wait, sn, cmd);
            return null;
        } catch (Exception e) {
            log.warn("[门禁WS] 下发失败 sn={} cmd={}: {}", sn, cmd, e.getMessage());
            return null;
        } finally {
            pending.remove(key(sn, token));
            if (StringUtils.isNotBlank(cmd)) {
                pending.remove(key(sn, "#" + cmd));
            }
        }
    }

    private boolean sendRaw(Session session, Map<String, Object> payload) {
        if (session == null || !session.isOpen()) {
            return false;
        }
        try {
            String text = MAPPER.writeValueAsString(payload);
            // javax.websocket 的 RemoteEndpoint.Basic 非线程安全, 按 session 串行化
            synchronized (session) {
                session.getBasicRemote().sendText(text);
            }
            String cmd = str(payload.get("cmd"));
            boolean isAck = payload.containsKey("result");
            if (!isAck) {
                log.info("[门禁WS] 下发 sn={} cmd={} payload={}", sessionSn.get(session), cmd, abbreviate(text));
            }
            return true;
        } catch (Exception e) {
            log.warn("[门禁WS] 发送失败 sessionId={}: {}", session.getId(), e.getMessage());
            return false;
        }
    }

    // ================= 状态查询 =================

    @Override
    public boolean isOnline(String sn) {
        Session s = sessions.get(sn);
        if (s == null || !s.isOpen()) {
            return false;
        }
        Long seen = lastSeen.get(sn);
        return seen != null && (System.currentTimeMillis() - seen) <= ONLINE_WINDOW_MS;
    }

    @Override
    public Long lastSeenMillis(String sn) {
        return lastSeen.get(sn);
    }

    @Override
    public Set<String> onlineDevices() {
        return Collections.unmodifiableSet(sessions.keySet());
    }

    // ================= 工具 =================

    private static String key(String sn, String correlation) {
        return (sn == null ? "" : sn) + "|" + (correlation == null ? "" : correlation);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 500 ? s.substring(0, 500) + "...(truncated)" : s;
    }

    private static String queryParam(Session session, String name) {
        try {
            String q = session.getQueryString();
            if (StringUtils.isBlank(q)) {
                return null;
            }
            for (String kv : q.split("&")) {
                int i = kv.indexOf('=');
                if (i > 0 && name.equals(kv.substring(0, i))) {
                    return kv.substring(i + 1);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
