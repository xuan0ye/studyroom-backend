package com.ruoyi.studyroom.wx.access;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.studyroom.wx.mqtt.DeviceMqttChannel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 门禁设备「引导」接口 —— 厂商协议的 {@code POST /server/access/}。
 *
 * <p><b>设备真实上线流程(逆向自厂商服务器实测应答)</b>:</p>
 * <ol>
 *   <li>设备每 ~5.5 秒 POST <code>{"access_mac":"&lt;MAC&gt;","access_type":"0A05","factory":"act"}</code></li>
 *   <li>服务器必须回 <code>code</code>(<b>字符串</b>, 成功为 "0"/"4004" 为参数错) 外加一整套
 *       MQTT 接入参数: mqtt_addr / mqtt_port / device_id / device_secret / mqtt_clientId /
 *       mqtt_sub / mqtt_pub / mqtt_ping_cli / mqtt_ping_svr, 以及 http_svr 与 qr_url</li>
 *   <li>设备随后连上该 MQTT broker, 订阅 iot/cli/&lt;type&gt;/&lt;mac&gt;/ 收命令,
 *       往 iot/svr/&lt;type&gt;/&lt;mac&gt;/ 发上报</li>
 * </ol>
 *
 * <p>此前我们的探针回的是 <code>{"code":0,"msg":"success"}</code> —— 整数 code 且<b>没有 MQTT 参数</b>,
 * 设备自然拿不到 broker, 只能无限轮询。本类改为按厂商字段回我们自己的 broker。</p>
 *
 * @author studyroom
 */
@Slf4j
@RestController
public class AccessBootstrapController {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {};

    /** 用 Redis 覆盖整个应答体, 便于不停机试协议: SET studyroom:access:bootstrap:reply '{...}' */
    public static final String OVERRIDE_KEY = "studyroom:access:bootstrap:reply";

    @Value("${studyroom.access.bootstrap.enabled:true}")
    private boolean enabled;

    /** 下发给设备的 broker 地址: 必须是设备能访问到的公网地址 */
    @Value("${studyroom.access.mqtt.public-host:}")
    private String publicHost;
    @Value("${studyroom.access.mqtt.public-port:1883}")
    private String publicPort;
    @Value("${studyroom.access.mqtt.username:}")
    private String mqttUsername;
    @Value("${studyroom.access.mqtt.password:}")
    private String mqttPassword;

    /** 下发给设备的 HTTP API 根地址(指向我们, 设备对它的调用会被探针抓下来) */
    @Value("${studyroom.access.bootstrap.http-svr:}")
    private String httpSvr;
    @Value("${studyroom.access.bootstrap.default-access-type:0A05}")
    private String defaultAccessType;
    /** 录脸二维码地址模板(保持指向厂商, 因其人脸数据仍在其云上) */
    @Value("${studyroom.access.bootstrap.qr-url-template:https://access.yefiot.com/act?c=%s}")
    private String qrUrlTemplate;

    /** 用于在设备上线后做一次只读探活(见 DeviceMqttChannel#onDeviceBootstrap) */
    private final ObjectProvider<DeviceMqttChannel> mqttChannelProvider;

    public AccessBootstrapController(ObjectProvider<DeviceMqttChannel> mqttChannelProvider) {
        this.mqttChannelProvider = mqttChannelProvider;
    }

    @PostMapping(value = "/server/access", produces = "application/json;charset=UTF-8")
    public String bootstrap(@RequestBody(required = false) String raw, HttpServletRequest request) {        String mac = null;
        String accessType = defaultAccessType;
        try {
            if (StringUtils.isNotBlank(raw)) {
                Map<String, Object> req = MAPPER.readValue(raw, MAP_TYPE);
                mac = str(req.get("access_mac"));
                String at = str(req.get("access_type"));
                if (StringUtils.isNotBlank(at)) {
                    accessType = at;
                }
            }
        } catch (Exception e) {
            log.warn("[门禁引导] 请求体解析失败: {}", raw);
        }
        log.info("[门禁引导] mac={} accessType={} from={}", mac, accessType, request.getRemoteAddr());

        if (!enabled) {
            return "{\"code\":\"4005\"}";
        }
        if (StringUtils.isBlank(mac)) {
            // 与厂商一致: 缺参数回 4004
            return "{\"code\":\"4004\"}";
        }
        Object override = null;
        try {
            override = RedisUtils.getCacheObject(OVERRIDE_KEY);
        } catch (Exception ignored) {
        }
        if (override != null && StringUtils.isNotBlank(String.valueOf(override))) {
            log.info("[门禁引导] 使用 Redis 覆盖应答");
            return String.valueOf(override);
        }

        Map<String, Object> r = new LinkedHashMap<>();
        r.put("code", "0");
        r.put("mqtt_addr", publicHost);
        r.put("mqtt_port", publicPort);
        r.put("device_secret", mqttPassword);
        r.put("device_id", mqttUsername);
        r.put("mqtt_clientId", mac);
        r.put("http_svr", httpSvr);
        r.put("mqtt_sub", "iot/cli/" + accessType + "/" + mac + "/");
        r.put("mqtt_ping_cli", "iot/cli/ping/" + mac + "/eth");
        r.put("mqtt_ping_svr", "iot/svr/ping/" + mac + "/eth");
        r.put("mqtt_pub", "iot/svr/" + accessType + "/" + mac + "/");
        r.put("qr_url", String.format(qrUrlTemplate, mac));
        try {
            String body = MAPPER.writeValueAsString(r);
            log.info("[门禁引导] 已下发 MQTT 配置: broker={}:{} sub={} pub={}",
                publicHost, publicPort, r.get("mqtt_sub"), r.get("mqtt_pub"));
            // 设备拿到配置后就会连 broker; 登记它并在稍后做一次只读探活,
            // 否则设备只在 MQTT 层发 PINGREQ, 业务层永远判它离线, 远程开门会被误拒。
            try {
                DeviceMqttChannel ch = mqttChannelProvider.getIfAvailable();
                if (ch != null) {
                    ch.onDeviceBootstrap(mac);
                }
            } catch (Exception e) {
                log.warn("[门禁引导] 登记探活失败: {}", e.getMessage());
            }
            return body;
        } catch (Exception e) {
            log.error("[门禁引导] 应答序列化失败", e);
            return "{\"code\":\"5000\"}";
        }
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}