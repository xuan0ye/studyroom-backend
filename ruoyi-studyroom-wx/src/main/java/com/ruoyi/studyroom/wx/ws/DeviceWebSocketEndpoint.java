package com.ruoyi.studyroom.wx.ws;

import lombok.extern.slf4j.Slf4j;

import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

/**
 * 门禁设备 WebSocket 端点(原生 JSR-356, 由 {@link DeviceWebSocketRegistrar} 程序化注册)。
 *
 * <p><b>两个必须记住的坑</b>:</p>
 * <ol>
 *   <li>即使走 {@code ServerContainer.addEndpoint()} 程序化注册, 生命周期方法也<b>必须</b>带
 *       {@code @OnOpen/@OnMessage/@OnClose/@OnError} 注解, 否则容器一个都不会调用
 *       (表现为握手成功但收不到任何回执)。</li>
 *   <li>JSR-356 默认文本消息上限仅 8KB, 而设备上报的人脸 JPG(Base64) 可达上百 KB,
 *       不放大上限会直接被容器拒收。这里放宽到 1MB。</li>
 * </ol>
 *
 * <p>端点实例由容器按连接创建, 无法依赖注入, 因此通过 {@link DeviceSessionHub#get()} 取得中枢。</p>
 *
 * @author studyroom
 */
@Slf4j
public class DeviceWebSocketEndpoint {

    /** 单条报文上限: 人脸 JPG(Base64) 可到上百 KB, 默认 8KB 会拒收 */
    private static final int MAX_MESSAGE_BYTES = 1024 * 1024;

    private Session session;

    @OnOpen
    public void onOpen(Session session) {
        this.session = session;
        try {
            session.setMaxTextMessageBufferSize(MAX_MESSAGE_BYTES);
            session.setMaxBinaryMessageBufferSize(MAX_MESSAGE_BYTES);
        } catch (Exception e) {
            log.warn("[门禁WS] 设置报文上限失败: {}", e.getMessage());
        }
        DeviceSessionHub hub = DeviceSessionHub.get();
        if (hub == null) {
            log.error("[门禁WS] 会话中枢未就绪, 关闭连接");
            closeQuietly(session);
            return;
        }
        hub.onOpen(session);
    }

    @OnMessage(maxMessageSize = MAX_MESSAGE_BYTES)
    public void onMessage(String message) {
        DeviceSessionHub hub = DeviceSessionHub.get();
        if (hub != null) {
            hub.onMessage(session, message);
        }
    }

    @OnClose
    public void onClose(Session session, CloseReason reason) {
        DeviceSessionHub hub = DeviceSessionHub.get();
        if (hub != null) {
            hub.onClose(session);
        }
        log.warn("[门禁WS] 连接关闭: {}", reason == null ? "?" : reason.getReasonPhrase());
    }

    @OnError
    public void onError(Session session, Throwable t) {
        DeviceSessionHub hub = DeviceSessionHub.get();
        if (hub != null) {
            hub.onError(session, t);
        }
    }

    private static void closeQuietly(Session s) {
        try {
            s.close();
        } catch (Exception ignored) {
        }
    }
}