package com.ruoyi.studyroom.wx.access;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.studyroom.service.IAccessControlService;
import com.ruoyi.studyroom.wx.mqtt.DeviceMqttChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 门禁设备「记录上报」接口 —— 厂商协议 v1.12 的 url/upRecord 一族。
 *
 * <p>设备开门的确认不走 MQTT: 设备开门成功后会 HTTP POST 一条记录到
 * &lt;http_svr&gt;/url/upRecord(陌生人 url/upStrangerRecord, 拍照 url/upCapturet)。
 * 我们下发的 http_svr 是 https://api.dayousuoan.top/access/v1/,
 * 所以真实落点形如 https://api.dayousuoan.top/access/v1/url/upRecord。</p>
 *
 * <p>报文(文档原文):</p>
 * <pre>
 * {"cmd":"F1upRecord","sn":"43434D340088078014190A02","body":{
 *     "name":"张三","jobnumber":1,"time":1613988402,"type":1,
 *     "temperature":36.5,"pic":"/9j/4AAQSkZJRgABAQAAAQABAAD/..."}}
 * 服务器应答: {"cmd":"upRecord","result":0,"errmsg":""}
 * </pre>
 *
 * <p>token 是本接口存在的关键理由: 文档注明 token「仅远程开机会返回」。
 * 服务器下发 F1openDoor 时带一个 token, 设备开门后把该 token 原样带回这条记录,
 * 于是"门到底开没开"有了真实回执 —— 这正是此前"指令已下发"其实没人开门的问题的解药。
 * 收到 token 后通过 DeviceMqttChannel#completePending 回灌给等待中的下发方。</p>
 *
 * <p>隐私: 报文里的 pic 是 base64 人脸/抓拍图, 只记长度不落库、不写日志。</p>
 *
 * @author studyroom
 */
@Slf4j
@RestController
public class AccessRecordController {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {};

    /** 报文上限, 防止超大 base64 打爆内存 */
    private static final int MAX_BODY_CHARS = 4 * 1024 * 1024;

    private final ObjectProvider<IAccessControlService> accessServiceProvider;
    private final ObjectProvider<DeviceMqttChannel> mqttChannelProvider;

    public AccessRecordController(ObjectProvider<IAccessControlService> accessServiceProvider,
                                  ObjectProvider<DeviceMqttChannel> mqttChannelProvider) {
        this.accessServiceProvider = accessServiceProvider;
        this.mqttChannelProvider = mqttChannelProvider;
    }

    /** 普通(注册人员)开门记录。兼容设备可能拼出的两种前缀。 */
    @PostMapping(value = {"/access/v1/url/upRecord", "/access/v1/upRecord"},
        produces = "application/json;charset=UTF-8")
    public String upRecord(@RequestBody(required = false) String raw, HttpServletRequest request) {
        return handle(raw, "upRecord", request);
    }

    /** 陌生人通行记录。 */
    @PostMapping(value = {"/access/v1/url/upStrangerRecord", "/access/v1/upStrangerRecord"},
        produces = "application/json;charset=UTF-8")
    public String upStrangerRecord(@RequestBody(required = false) String raw, HttpServletRequest request) {
        return handle(raw, "upStrangerRecord", request);
    }

    /** 拍照上传。 */
    @PostMapping(value = {"/access/v1/url/upCapturet", "/access/v1/upCapturet", "/access/v1/url/upCapture"},
        produces = "application/json;charset=UTF-8")
    public String upCapturet(@RequestBody(required = false) String raw, HttpServletRequest request) {
        return handle(raw, "upCapturet", request);
    }

    private String handle(String raw, String ackCmd, HttpServletRequest request) {
        String path = request == null ? "?" : request.getRequestURI();
        String from = request == null ? "?" : request.getRemoteAddr();
        if (StringUtils.isBlank(raw)) {
            log.warn("[门禁上报] 空报文 path={} from={}", path, from);
            return ack(ackCmd);
        }
        if (raw.length() > MAX_BODY_CHARS) {
            log.warn("[门禁上报] 报文过大被拒 path={} from={} len={}", path, from, raw.length());
            return "{\"cmd\":\"" + ackCmd + "\",\"result\":1,\"errmsg\":\"payload too large\"}";
        }

        Map<String, Object> payload = null;
        try {
            payload = MAPPER.readValue(raw, MAP_TYPE);
        } catch (Exception e) {
            log.warn("[门禁上报] 报文非 JSON path={} from={} len={}", path, from, raw.length());
        }
        if (payload == null) {
            payload = new LinkedHashMap<>();
        }
        DeviceMqttChannel.normalizeIncoming(payload, null);

        String sn = str(payload.get("sn"));
        String token = str(payload.get("token"));
        Object body = payload.get("body");
        if (StringUtils.isBlank(token) && body instanceof Map) {
            token = str(((Map<?, ?>) body).get("token"));
        }
        String name = null;
        if (body instanceof Map) {
            name = str(((Map<?, ?>) body).get("name"));
        }
        log.info("[门禁上报] {} path={} sn={} token={} name={} type={} from={} len={}",
            ackCmd, path, sn, token, name,
            body instanceof Map ? str(((Map<?, ?>) body).get("type")) : null,
            from, raw.length());

        if (StringUtils.isNotBlank(token)) {
            try {
                DeviceMqttChannel ch = mqttChannelProvider.getIfAvailable();
                if (ch != null && ch.completePending(sn, token, payload)) {
                    log.info("[门禁上报] 已确认远程开门成功 sn={} token={}", sn, token);
                }
            } catch (Exception e) {
                log.warn("[门禁上报] 回灌等待方失败: {}", e.getMessage());
            }
        }

        try {
            IAccessControlService svc = accessServiceProvider.getIfAvailable();
            if (svc != null) {
                svc.handleMqttDataReport(payload);
            }
        } catch (Exception e) {
            log.warn("[门禁上报] 业务处理失败: {}", e.getMessage());
        }
        return ack(ackCmd);
    }

    private static String ack(String cmd) {
        return "{\"cmd\":\"" + cmd + "\",\"result\":0,\"errmsg\":\"\"}";
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}