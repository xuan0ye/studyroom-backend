package com.ruoyi.studyroom.wx.access;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 门禁设备登录接口 —— 厂商《云端MQTT协议精简版 v1.12》的 {@code POST /login}。
 *
 * <p><b>协议要点(逐字对照文档 v1.12)</b>:</p>
 * <ul>
 *   <li>设备每次开机都请求一次; 路径写在设备「通信设置 → 服务器地址」后面(如 {@code 192.168.3.10/login})</li>
 *   <li>设备请求: {@code {"cmd":"login","sn":"<设备序列号>"}}</li>
 *   <li>服务器必须回: cmd / url / host / port / user / pd / uploadTopic / downTopic / clientID / keepalive</li>
 *   <li>之后命令走 MQTT(downTopic 下发, uploadTopic 回执)<b>全明文 JSON</b>; 命令名为 {@code F1*} 前缀</li>
 *   <li><b>开门记录不走的 MQTT</b>, 而是设备 HTTP POST 到 {@code url/upRecord}(陌生人 url/upStrangerRecord, 拍照 url/upCapturet)</li>
 * </ul>
 *
 * @author studyroom
 */
@Slf4j
@RestController
public class AccessLoginController {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {};

    /** Redis 覆盖应答, 便于不停机试协议 */
    public static final String OVERRIDE_KEY = "studyroom:access:login:reply";

    /** MQTT broker 地址(不带 http), 下发给设备 */
    @Value("${studyroom.access.mqtt.public-host:}")
    private String publicHost;
    @Value("${studyroom.access.mqtt.public-port:1883}")
    private String publicPort;
    @Value("${studyroom.access.mqtt.username:}")
    private String mqttUser;
    @Value("${studyroom.access.mqtt.password:}")
    private String mqttPassword;

    /** 记录上传基址: 设备会往 <url>/upRecord、<url>/upStrangerRecord、<url>/upCapturet POST */
    @Value("${studyroom.access.bootstrap.record-url:}")
    private String recordUrl;
    @Value("${studyroom.access.bootstrap.keepalive:90}")
    private int keepalive;
    @Value("${studyroom.access.bootstrap.enabled:true}")
    private boolean enabled;

    /**
     * 注意: <b>不能</b>用文档里的裸 {@code /login} —— 那个已被 RuoYi 后台的 SysLoginController 占用,
     * 再加一个会 Ambiguous mapping 直接导致启动失败。文档明确「路径写在设备服务器地址后面」,
     * 故改用 {@code /access/login}: nginx 已转发该前缀, 且已在 security.excludes 放行名单内。
     */
    @PostMapping(value = "/access/login", produces = "application/json;charset=UTF-8")
    public Map<String, Object> login(@RequestBody(required = false) String raw, HttpServletRequest request) {
        String sn = null;
        String cmd = null;
        try {
            if (StringUtils.isNotBlank(raw)) {
                Map<String, Object> req = MAPPER.readValue(raw, MAP_TYPE);
                sn = str(req.get("sn"));
                cmd = str(req.get("cmd"));
            }
        } catch (Exception e) {
            log.warn("[门禁登录] 请求体解析失败: {}", raw);
        }
        log.info("[门禁登录] cmd={} sn={} from={}", cmd, sn, request.getRemoteAddr());

        // 保险: 若设备固件不认这套精简版协议(仍发 access_mac 那套), 回落到 /server/access 用的厂商覆盖值,
        // 避免把已经恢复正常的厂商云连接弄坏。
        if (!"login".equalsIgnoreCase(String.valueOf(cmd))) {
            try {
                Object fb = RedisUtils.getCacheObject(AccessBootstrapController.OVERRIDE_KEY);
                if (fb != null && StringUtils.isNotBlank(String.valueOf(fb))) {
                    log.warn("[门禁登录] 请求不是 cmd:login(实际 cmd={}), 回落到厂商覆盖配置", cmd);
                    return MAPPER.readValue(String.valueOf(fb), MAP_TYPE);
                }
            } catch (Exception e) {
                log.warn("[门禁登录] 回落读取失败: {}", e.getMessage());
            }
        }

        // Redis 覆盖优先(便于不停机切换/试协议)
        try {
            Object ov = RedisUtils.getCacheObject(OVERRIDE_KEY);
            if (ov != null && StringUtils.isNotBlank(String.valueOf(ov))) {
                Map<String, Object> r = MAPPER.readValue(String.valueOf(ov), MAP_TYPE);
                if (sn != null && !"null".equals(String.valueOf(r.get("clientID")))) {
                    r.put("clientID", sn);
                }
                log.info("[门禁登录] 使用 Redis 覆盖应答");
                return r;
            }
        } catch (Exception e) {
            log.warn("[门禁登录] 读取覆盖失败: {}", e.getMessage());
        }

        String mac = StringUtils.isBlank(sn) ? "unknown" : sn;
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("cmd", "login");
        r.put("host", publicHost);
        r.put("port", publicPort);
        r.put("user", mqttUser);
        r.put("pd", mqttPassword);
        // 设备上报主题(设备 -> 服务器)
        r.put("uploadTopic", "cs/" + mac + "/csevents");
        // 服务器下发主题(服务器 -> 设备)
        r.put("downTopic", "cs/" + mac + "/csmsg");
        r.put("clientID", mac);
        r.put("keepalive", keepalive);
        r.put("url", recordUrl);
        if (!enabled) {
            r.clear();
            r.put("cmd", "login");
            r.put("errmsg", "disabled");
        }
        log.info("[门禁登录] 已下发 MQTT 配置: broker={}:{} up={} down={}",
            publicHost, publicPort, r.get("uploadTopic"), r.get("downTopic"));
        return r;
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }
}