package com.ruoyi.studyroom.wx.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.studyroom.domain.AccessDevice;
import com.ruoyi.studyroom.mapper.AccessDeviceMapper;
import com.ruoyi.studyroom.service.IAccessControlService;
import com.ruoyi.studyroom.service.IDeviceChannel;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * 门禁设备 MQTT 通道(厂商《云端MQTT协议精简版 v1.12》)。
 *
 * <p>设备流程: 开机 HTTP POST {@code {"cmd":"login","sn":...}} 拿 MQTT 配置 →
 * 连 broker → 订阅 downTopic 收命令、往 uploadTopic 回执。</p>
 *
 * <p>本类负责: 连本机 mosquitto、订阅 {@code cs/+/csevents}、把命令下发到 {@code cs/<sn>/csmsg},
 * 并<b>等待设备 result 回执</b>才判定成功(这是"假成功"问题的根治点)。</p>
 *
 * <p>与 {@link com.ruoyi.studyroom.wx.ws.DeviceSessionHub}(WebSocket 备选通道)实现同一个
 * {@link IDeviceChannel}, 由 AccessControlServiceImpl 择优选用。</p>
 *
 * @author studyroom
 */
@Slf4j
@Component
@org.springframework.core.annotation.Order(0)
public class DeviceMqttChannel implements IDeviceChannel {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<Map<String, Object>>() {};

    /** 设备上报主题通配(v1.12 文档协议: /access/login 引导的设备): cs/<sn>/csevents */
    private static final String SUB_TOPIC_CS = "cs/+/csevents";
    /**
     * 设备上报主题通配(厂商实际协议: /server/access 引导的设备)。
     *
     * <p>厂商 broker 实测: 设备上报走 {@code iot/svr/<type>/<sn>/eth}(type 形如 0A05),
     * 服务器下发走 {@code iot/cli/<type>/<sn>/eth}。我方 /server/access 下发的 mqtt_pub/mqtt_sub
     * 正是这一族, 所以必须订阅它, 否则设备连上了也"发了没人收"。</p>
     */
    private static final String SUB_TOPIC_IOT = "iot/svr/+/+/eth";
    /** 实际订阅的全部上报主题 */
    private static final String[] SUB_TOPICS = { SUB_TOPIC_CS, SUB_TOPIC_IOT };

    /** 下发主题模板(v1.12 文档协议): cs/<sn>/csmsg */
    private static final String PUB_TOPIC_CS_TMPL = "cs/%s/csmsg";
    /** 下发主题模板(厂商实际协议): iot/cli/<type>/<sn>/eth */
    private static final String PUB_TOPIC_IOT_TMPL = "iot/cli/%s/%s/eth";

    /**
     * cmd → 厂商 action 名映射(仅收录已在实机/厂商 broker 实测确认的名字)。
     *
     * <p>{@code open_door} 这个 action 名<b>不下发</b>。2026-10-09 复核后的完整理由:</p>
     * <ul>
     *   <li>它在厂商 broker 上只出现在<b>设备→App 方向</b>(带 {@code "to":"app"}、
     *       {@code code:"0"}), 是设备"门已开"的<b>上报事件</b>, 不是开门口令;
     *       全量 {@code iot/cli/#} 抓包里服务器→设备方向出现 <b>0 次</b>;</li>
     *   <li>我方实机发它, 设备回 {@code code:"1"}(失败);</li>
     *   <li>它会被 {@link #normalizeAck} 翻译成 result, <b>抢在真正的 cs 回执之前唤醒等待方</b>,
     *       造成"门开了却报失败"或反之。</li>
     * </ul>
     *
     * <p>{@code F1openDoor} 同样<b>不放进来</b>: 那是 {@code protocolVersion=1} 标准协议的指令,
     * 本机是 0(时之盾), 实测发过去只回 {@code code:"7"}(不认该动作)。
     * 结论与开关 {@link #mqttOpenEnabled} 一致: 本机无法用服务器指令开门。</p>
     */
    private static final Map<String, String> CMD_TO_ACTION;
    static {
        Map<String, String> m = new ConcurrentHashMap<>();
        // 键统一大写, 查找时对 cmd 做 toUpperCase。
        // F1OPENDOOR 刻意不在表里: 本机协议版本不认(实测 code 7), 见上。
        // 只读探针(用来确认设备在线/建立 lastSeen, 不会产生任何物理动作)
        m.put("F1READPARAM", "read_param");
        CMD_TO_ACTION = Collections.unmodifiableMap(m);
    }

    /** 报文方向标识: 服务器侧固定为 "svr", 用它区分 from/to 谁是设备 */
    private static final String SVR = "svr";

    /** 设备"在线"判定窗口: 超过 3 个 keepalive(90s) 没动静就算掉线 */
    private static final long ONLINE_WINDOW_MS = 270_000L;

    public static final long DEFAULT_ACK_TIMEOUT_MS = 8_000L;

    @Value("${studyroom.access.mqtt.host:tcp://127.0.0.1:1883}")
    private String broker;
    @Value("${studyroom.access.mqtt.username:}")
    private String username;
    @Value("${studyroom.access.mqtt.password:}")
    private String password;
    @Value("${studyroom.access.bootstrap.keepalive:90}")
    private int keepalive;
    /** 设备型号段(iot/<方向>/<type>/<sn>/eth 里的 type), 与 /server/access 下发的 access_type 保持一致 */
    @Value("${studyroom.access.bootstrap.default-access-type:0A05}")
    private String accessType;
    /** 是否启用(排查时可临时关掉, 免得与其它通道打架) */
    @Value("${studyroom.access.mqtt.client-enabled:true}")
    private boolean enabled;

    /**
     * cmd 报文({@code F1*} 标准协议)下发到哪些主题: {@code cs} / {@code eth} / {@code both}, 默认 {@code both}。
     *
     * <p>为什么必须可配, 又为什么默认两个都发: 设备订阅哪个主题, 是<b>我们自己在引导应答里告诉它的</b> ——</p>
     * <ul>
     *   <li>{@code /access/login} 回 {@code downTopic = cs/<sn>/csmsg};</li>
     *   <li>{@code /server/access} 回 {@code mqtt_sub = iot/cli/<type>/<sn>/}。</li>
     * </ul>
     * <p>本机固件走的是后者(nginx 日志: 它一直在 POST /server/access), 即订阅 {@code iot/cli/.../eth}。
     * 所以设备把系统参数 {@code protocolVersion} 从 0 改成 1(标准协议)之后, 它到底还在哪个主题上说话,
     * 事前无法确定 —— <b>默认两个都发就不要这个前提</b>: 谁收到谁执行, 另一条顶多回一个
     * {@code {"action":"","data":[{"code":"2"}]}}(解析失败)。那种回包没有 token/serialno、也没有 cmd 名,
     * 匹配不上任何等待方(见 {@link #onMessage}), 所以不会造成"假成功"。</p>
     *
     * <p>确认设备实际订阅后, 可以收紧成 {@code cs} 或 {@code eth} 少发一条。</p>
     */
    @Value("${studyroom.access.mqtt.cmd-topic:both}")
    private String cmdTopicMode;

    /** 本次 cmd 报文该发往的主题列表(见 {@link #cmdTopicMode}) */
    private java.util.List<String> cmdTopics(String sn) {
        String mode = cmdTopicMode == null ? "both" : cmdTopicMode.trim().toLowerCase();
        java.util.List<String> topics = new java.util.ArrayList<>(2);
        if ("cs".equals(mode) || "both".equals(mode)) {
            topics.add(String.format(PUB_TOPIC_CS_TMPL, sn));
        }
        if ("eth".equals(mode) || "both".equals(mode)) {
            topics.add(String.format(PUB_TOPIC_IOT_TMPL, accessType, sn));
        }
        if (topics.isEmpty()) {
            topics.add(String.format(PUB_TOPIC_CS_TMPL, sn));
        }
        return topics;
    }

    /**
     * 是否允许"服务器远程开门"。<b>默认 false —— 这台设备当前做不到, 别把它当真打开。</b>
     *
     * <p>2026-10-09 实测: 三条独立证据指向同一结论。</p>
     * <ol>
     *   <li><b>通道到不了设备</b>: mosquitto 的 SUBSCRIBE 实录显示, 设备(clientId = SN)历史上
     *       只订阅 {@code iot/cli/0A05/<SN>/eth} 与 {@code iot/cli/FFFF/<SN>/eth},
     *       <b>从未订阅 {@code cs/}</b>。我们往 {@code cs/<SN>/csmsg} 发过的报文,
     *       broker 日志里连一次 "Sending PUBLISH to &lt;SN&gt;" 都没有 —— 文档协议的通道是死的。</li>
     *   <li><b>设备不认这套指令</b>: 把文档里的 action 名(F1openDoor / F1readParam /
     *       F1setSysCommonParam)发到设备真正订阅的 {@code iot/cli/.../eth}, 一律回
     *       {@code code:"7"}; 换成 cs 的 cmd 报文则回 {@code code:"2"}(解析失败)。</li>
     *   <li><b>根因写在厂商文档自己身上</b>: 系统参数 {@code protocolVersion}
     *       <b>0=时之盾(本机现状) / 1=标准协议</b>, 文档原文"若要使用此文档中的协议该字段需设置为 1"。
     *       本机是 0, 所以文档里的远程开门对这台机器不成立; 厂商 App 能开门是因为它走
     *       时之盾协议的加密信封({@code {"code":"<不透明签名串>"}}, 密钥在厂商手里)。</li>
     * </ol>
     *
     * <p>只有这两种情况才该打开本开关: ① 现场把设备 {@code protocolVersion} 改成 1 并重启,
     * 且确认设备仍连我方 broker、订阅了 {@code cs/} 主题; ② 换成支持标准协议的固件/设备。
     * 另一条不依赖设备的路是 {@code studyroom.door.open-url} 的继电器执行器。</p>
     */
    @Value("${studyroom.door.mqtt-open-enabled:false}")
    private boolean mqttOpenEnabled;

    private final ObjectProvider<IAccessControlService> accessServiceProvider;
    private final ObjectProvider<AccessDeviceMapper> deviceMapperProvider;
    private final Map<String, Long> lastSeen = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<Map<String, Object>>> pending = new ConcurrentHashMap<>();
    /** 已通过 /server/access 引导过的设备序列号, 用于周期探活 */
    private final Set<String> knownSn = ConcurrentHashMap.newKeySet();
    /** 设备不透明报文({code:...})的累计条数, 只用于汇报频率, 不落库 */
    private final java.util.concurrent.atomic.AtomicLong opaqueCount = new java.util.concurrent.atomic.AtomicLong();

    private MqttClient client;
    private volatile boolean connected = false;

    public DeviceMqttChannel(ObjectProvider<IAccessControlService> accessServiceProvider,
                             ObjectProvider<AccessDeviceMapper> deviceMapperProvider) {
        this.accessServiceProvider = accessServiceProvider;
        this.deviceMapperProvider = deviceMapperProvider;
    }

    @PostConstruct
    public void start() {
        if (!enabled) {
            log.warn("[门禁MQTT] 已禁用(studyroom.access.mqtt.client-enabled=false)");
            return;
        }
        new Thread(this::connectLoop, "device-mqtt-init").start();
        new Thread(this::heartbeatLoop, "device-mqtt-heartbeat").start();
    }

    /**
     * 设备刚完成 /server/access 引导(即将连上 broker)时调用。
     *
     * <p>设备只在 MQTT 协议层发 PINGREQ, 那属于 broker 内部报文, 后端看不见,
     * 于是"连着的设备"在业务层 lastSeen 永远是空 -> isOnline=false -> 远程开门被误判为设备离线。
     * 因此引导后延迟一小会儿发一次<b>只读探针</b>(read_param)把 lastSeen 建立起来。</p>
     */
    public void onDeviceBootstrap(String sn) {
        if (StringUtils.isBlank(sn)) {
            return;
        }
        knownSn.add(sn);
        new Thread(() -> {
            try {
                Thread.sleep(12_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            probe(sn, 6_000L);
        }, "device-probe-" + sn).start();
    }

    /** 从数据库补登记已知设备(后端重启后 knownSn 会是空的) */
    private void seedKnownDevices() {
        try {
            AccessDeviceMapper mapper = deviceMapperProvider.getIfAvailable();
            if (mapper == null) {
                return;
            }
            List<AccessDevice> devices = mapper.selectList(null);
            if (devices != null) {
                for (AccessDevice d : devices) {
                    if (d != null && StringUtils.isNotBlank(d.getSn())) {
                        knownSn.add(d.getSn());
                    }
                }
            }
            if (!knownSn.isEmpty()) {
                log.info("[门禁MQTT] 已从库中登记 {} 台设备用于探活: {}", knownSn.size(), knownSn);
            }
        } catch (Exception e) {
            log.warn("[门禁MQTT] 从库中登记设备失败: {}", e.getMessage());
        }
    }

    /** 周期性探活: 对久未活跃的已知设备发只读探针, 维持在线判定的准确性 */
    private void heartbeatLoop() {        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(60_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            for (String sn : knownSn) {
                Long seen = lastSeen.get(sn);
                if (seen != null && System.currentTimeMillis() - seen < 120_000L) {
                    continue;
                }
                probe(sn, 6_000L);
            }
        }
    }

    /**
     * 把在线时间落库。
     *
     * <p>原先只有心跳类业务上报才会写 {@code sr_access_device.last_online_time}, 而 MQTT 通道自身
     * 从不写库 —— 于是设备明明一直在线, 库里那个字段却停在很久以前。
     * 一旦实时覆盖因故没生效(例如档案里没写 device_sn), 页面就会显示"已离线 N 天"。
     * 探活成功顺手写一次, 让兜底值也是新鲜的。</p>
     */
    private void touchDeviceDb(String sn) {
        try {
            AccessDeviceMapper mapper = deviceMapperProvider.getIfAvailable();
            if (mapper == null) {
                return;
            }
            com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AccessDevice> lqw =
                com.baomidou.mybatisplus.core.toolkit.Wrappers.lambdaQuery();
            lqw.eq(AccessDevice::getSn, sn);
            AccessDevice d = mapper.selectOne(lqw);
            if (d != null) {
                d.setStatus(1);
                d.setLastOnlineTime(new java.util.Date());
                mapper.updateById(d);
            }
        } catch (Exception e) {
            log.debug("[门禁MQTT] 写在线时间到库失败 sn={}: {}", sn, e.getMessage());
        }
    }

    /** 只读探针: 刷新 lastSeen, <b>绝不产生任何物理动作</b>(read_param 仅读参数) */
    private void probe(String sn, long timeoutMs) {
        if (!connected) {
            return;
        }
        try {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("cmd", "F1readParam");
            p.put("sn", sn);
            Map<String, Object> ack = sendAndAwait(sn, p, timeoutMs);
            if (ack != null) {
                // 只要设备回了包就说明在线; read_param 的 code 不是"成功/失败"语义, 不作为判据
                lastSeen.put(sn, System.currentTimeMillis());
                touchDeviceDb(sn);
                log.info("[门禁MQTT] 探针有回应(设备在线) sn={} code={}", sn, ack.get("deviceCode"));
            } else {
                log.info("[门禁MQTT] 探针无回应 sn={}", sn);
            }
        } catch (Exception e) {
            log.warn("[门禁MQTT] 探针异常 sn={}: {}", sn, e.getMessage());
        }
    }

    private void connectLoop() {
        int attempt = 0;
        while (client == null && attempt++ < 10) {
            try {
                String clientId = "studyroom-svr-" + UUID.randomUUID().toString().substring(0, 8);
                client = new MqttClient(broker, clientId, new MemoryPersistence());
                MqttConnectOptions opts = new MqttConnectOptions();
                opts.setCleanSession(true);
                opts.setKeepAliveInterval(keepalive > 0 ? keepalive : 60);
                opts.setConnectionTimeout(10);
                opts.setAutomaticReconnect(true);
                if (StringUtils.isNotBlank(username)) {
                    opts.setUserName(username);
                }
                if (StringUtils.isNotBlank(password)) {
                    opts.setPassword(password.toCharArray());
                }
                client.setCallback(new MqttCallbackExtended() {
                    @Override
                    public void connectComplete(boolean reconnect, String serverURI) {
                        connected = true;
                        log.info("[门禁MQTT] 已连接 {} (reconnect={})", serverURI, reconnect);
                        try {
                            for (String t : SUB_TOPICS) {
                                client.subscribe(t, 1);
                                log.info("[门禁MQTT] 已订阅 {}", t);
                            }
                        } catch (Exception e) {
                            log.error("[门禁MQTT] 订阅失败: {}", e.getMessage());
                        }
                    }

                    @Override
                    public void connectionLost(Throwable cause) {
                        connected = false;
                        log.warn("[门禁MQTT] 连接断开: {}", cause == null ? "?" : cause.getMessage());
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        onMessage(topic, new String(message.getPayload(), StandardCharsets.UTF_8));
                    }

                    @Override
                    public void deliveryComplete(IMqttDeliveryToken token) {
                    }
                });
                client.connect(opts);
                log.info("[门禁MQTT] 初始化连接成功: {}", broker);
                // 后端一重启, 内存里的 knownSn 就空了; 而设备只要还连着就不会重新引导,
                // 于是它会一直被误判离线、远程开门被拒。所以启动后立刻从库里补登记并探活。
                new Thread(() -> {
                    try {
                        Thread.sleep(5000L);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    seedKnownDevices();
                    for (String sn : knownSn) {
                        probe(sn, 6_000L);
                    }
                }, "device-seed-probe").start();
                return;
            } catch (Exception e) {
                log.warn("[门禁MQTT] 连接失败(第 {} 次): {}", attempt, e.getMessage());
                try {
                    Thread.sleep(5000L);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        }
        if (client == null) {
            log.error("[门禁MQTT] 10 次重试后仍无法连接 {}, MQTT 通道不可用", broker);
        }
    }

    @PreDestroy
    public void stop() {
        try {
            if (client != null && client.isConnected()) {
                client.disconnect();
            }
            if (client != null) {
                client.close();
            }
        } catch (Exception ignored) {
        }
    }

    /** 设备上报分发: 兼容 v1.12 文档协议(cs/&lt;sn&gt;/csevents + cmd) 与厂商实际协议(iot/svr/&lt;type&gt;/&lt;sn&gt;/eth + action) */
    @SuppressWarnings("unchecked")
    private void onMessage(String topic, String text) {
        String sn = snFromTopic(topic);
        if (StringUtils.isNotBlank(sn)) {
            lastSeen.put(sn, System.currentTimeMillis());
        }
        Map<String, Object> msg;
        try {
            msg = MAPPER.readValue(text, MAP_TYPE);
        } catch (Exception e) {
            log.warn("[门禁MQTT] 报文非 JSON sn={} topic={} text={}", sn, topic, abbreviate(text));
            return;
        }
        normalizeIncoming(msg, sn);
        String cmd = str(msg.get("cmd"));
        // 设备会以极高频率重发一种"没有 action、只有一个自定义 base64 不透明块 {code:...}"的报文,
        // 每条 231 字节、几百条地刷屏。它既解析不出语义, 还会被当成业务上报写进事件日志,
        // 结果就是把 add_face/add_user 的真实回执彻底淹没, 排查时什么都看不见。
        // 这里单独降级: 不打逐条日志、不落库, 只做计数, 每 200 条汇报一次频率。
        if (StringUtils.isBlank(cmd) && msg.get("code") != null && msg.size() <= 2) {
            long n = opaqueCount.incrementAndGet();
            if (n % 200 == 0) {
                log.warn("[门禁MQTT] 设备不透明报文已累计 {} 条(sn={}), 已降级为只计数不落库", n, sn);
            }
            return;
        }
        log.info("[门禁MQTT] 收到 sn={} cmd={} topic={} len={} text={}", sn, cmd, topic, text.length(), abbreviate(text));

        // 0) 设备 action 协议用 data[0].code 表结果, 先翻译成业务层认的 result
        normalizeAck(msg);

        // 1) 命令回执 => 唤醒等待方。关联键优先用我们下发的 serialno(设备原样带回),
        //    文档协议(log cs/<sn>/csmsg)则用 token。
        if (msg.containsKey("result")) {
            CompletableFuture<Map<String, Object>> f = null;
            String corr = str(msg.get("serialno"));
            if (StringUtils.isBlank(corr)) {
                corr = str(msg.get("token"));
            }
            if (StringUtils.isNotBlank(corr)) {
                f = pending.get(key(sn, corr));
            }
            if (f == null && cmd != null) {
                f = pending.get(key(sn, "#" + cmd.toUpperCase()));
            }
            if (f != null) {
                f.complete(msg);
                return;
            }
            log.info("[门禁MQTT] 无匹配回执(可能已超时) sn={} cmd={} result={}", sn, cmd, msg.get("result"));
            return;
        }

        // 2) 其它为设备主动上报(开门记录等) => 交业务处理
        //    但"查询类"回包绝不能当上报: read_face 一次回 679KB、里面是上百张人脸 base64,
        //    一旦落进 sr_access_event_log.raw_payload 就是把生物特征写进了数据库。
        //    查询回包已在上面的回执分支返回; 这里再兜一道, 防的是"回执超时后才到达"的漏网情况。
        // 例外: read_record 恰恰是设备"推送通行记录"的动作(厂商流量里它 31 次都是主动推送),
        // 一刀切会把到店签到的数据源挡掉。其余 read_* 都是查询回包, 不该落业务库。
        boolean isRecordPush = "read_record".equalsIgnoreCase(cmd == null ? "" : cmd.trim());
        if (isQueryAction(cmd) && !isRecordPush) {
            return;
        }
        try {
            IAccessControlService svc = accessServiceProvider.getIfAvailable();
            if (svc != null) {
                svc.handleMqttDataReport(msg);
            }
        } catch (Exception e) {
            log.warn("[门禁MQTT] 上报业务处理失败 cmd={}: {}", cmd, e.getMessage());
        }
    }

    /** 查询类动作(read_* / gettime / ping / pong / reg_device)只作回执, 不作为业务上报 */
    static boolean isQueryAction(String cmd) {
        if (StringUtils.isBlank(cmd)) {
            return false;
        }
        String c = cmd.trim().toLowerCase();
        return c.startsWith("read") || c.startsWith("f1get") || c.startsWith("f1read")
            || "gettime".equals(c) || "ping".equals(c) || "pong".equals(c) || "reg_device".equals(c);
    }

    /**
     * 从主题解析设备序列号, 按主题族取不同位置。
     *
     * <ul>
     *   <li>{@code cs/<sn>/csevents} → seg[1]</li>
     *   <li>{@code iot/svr/<type>/<sn>/eth} → seg[3]</li>
     * </ul>
     */
    static String snFromTopic(String topic) {
        if (StringUtils.isBlank(topic)) {
            return null;
        }
        String[] seg = topic.split("/");
        if (topic.startsWith("iot/")) {
            // iot/svr/<type>/<sn>/eth  (或 iot/svr/ping/<sn>/eth)
            return seg.length >= 4 ? seg[3] : null;
        }
        return seg.length >= 2 ? seg[1] : null;
    }

    /**
     * 把两种协议的报文字段归一化成业务层认识的形状。
     *
     * <p>厂商实际协议是 {@code {action, data, from, serialno, to, ts}}, 既没有 cmd 也没有 sn;
     * 业务层只认 cmd/sn/body, 所以在这里补齐, 避免设备上报被静默丢弃。</p>
     */
    @SuppressWarnings("unchecked")
    public static void normalizeIncoming(Map<String, Object> msg, String snFromTopic) {
        if (msg == null) {
            return;
        }
        // cmd ← action
        if (msg.get("cmd") == null && msg.get("action") != null) {
            msg.put("cmd", String.valueOf(msg.get("action")));
        }
        // sn ← 主题解析值优先; 无主题时再猜字段。
        // 注意方向: 设备->服务器时 to="svr" 而 from 才是 SN, 取错会把 sn 写成 "svr"。
        if (msg.get("sn") == null || StringUtils.isBlank(String.valueOf(msg.get("sn")))) {
            String sn = null;
            if (StringUtils.isNotBlank(snFromTopic)) {
                sn = snFromTopic;
            } else {
                String to = str(msg.get("to"));
                String from = str(msg.get("from"));
                if (StringUtils.isNotBlank(to) && !SVR.equalsIgnoreCase(to)) {
                    sn = to;
                } else if (StringUtils.isNotBlank(from) && !SVR.equalsIgnoreCase(from)) {
                    sn = from;
                }
            }
            if (sn != null) {
                msg.put("sn", sn);
            }
        }
        // body ← data(厂商 action 协议的载荷在 data 里)
        if (msg.get("body") == null && msg.get("data") != null) {
            Object data = msg.get("data");
            if (data instanceof Map) {
                msg.put("body", data);
            } else if (data instanceof java.util.List && !((java.util.List<?>) data).isEmpty()) {
                Object first = ((java.util.List<?>) data).get(0);
                if (first instanceof Map) {
                    msg.put("body", first);
                }
            }
        }
    }

    /**
     * 把设备 action 协议的回执位压成业务层认识的 {@code result}。
     *
     * <p>实测: 设备回包形如 {@code {"action":"add_face","serialno":"<原样带回>",
     * "from":"<sn>","to":"svr","data":[{"code":"0"}]}} —— 没有 result 字段。</p>
     *
     * <p><b>code 语义以厂商 broker 的真实流量为准</b>: {@code add_face} 成功 55 次全是
     * {@code code:"0"}, 失败 7 次全是 {@code code:"1"}; {@code add_user}/{@code del_user}/
     * {@code del_face}/{@code open_door} 成功也都回 {@code code:"0"}。
     * 所以判定规则是 <b>只有 "0" 算成功</b>, 其余一律按失败如实上报 —— 绝不能反过来
     * 把 "1" 当成功, 否则人脸下发失败会被谎报成"已同步"。</p>
     */
    static void normalizeAck(Map<String, Object> msg) {
        if (msg == null || msg.containsKey("result")) {
            return;
        }
        Object code = null;
        Object body = msg.get("body");
        if (body instanceof Map) {
            code = ((Map<?, ?>) body).get("code");
        }
        if (code == null && msg.get("data") instanceof java.util.List) {
            java.util.List<?> list = (java.util.List<?>) msg.get("data");
            for (Object o : list) {
                if (o instanceof Map && ((Map<?, ?>) o).get("code") != null) {
                    code = ((Map<?, ?>) o).get("code");
                    break;
                }
            }
        }
        if (code == null) {
            // read_user / read_face 这类查询回包直接带数据, 没有 code, 视为成功
            if (msg.get("data") instanceof java.util.List && !((java.util.List<?>) msg.get("data")).isEmpty()) {
                msg.put("result", "0");
            }
            return;
        }
        String c = String.valueOf(code).trim();
        msg.put("deviceCode", c);
        msg.put("result", "0".equals(c) ? "0" : c);
    }

    private static String str(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    @Override
    public Map<String, Object> sendAndAwait(String sn, Map<String, Object> payload, long timeoutMs) {
        if (StringUtils.isBlank(sn) || payload == null) {
            return null;
        }
        if (client == null || !client.isConnected()) {
            log.warn("[门禁MQTT] 未连接 broker, 无法下发 sn={} cmd={}", sn, payload.get("cmd"));
            return null;
        }
        String cmd = str(payload.get("cmd"));
        // 关联键: 设备会把下发的 serialno 原样带回, 所以它就是我们唯一的回执凭据。
        String corr = str(payload.get("token"));
        if (StringUtils.isBlank(corr)) {
            corr = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        payload.put("token", corr);
        CompletableFuture<Map<String, Object>> future = new CompletableFuture<>();
        pending.put(key(sn, corr), future);
        if (cmd != null) {
            pending.put(key(sn, "#" + cmd.toUpperCase()), future);
        }
        long wait = timeoutMs > 0 ? timeoutMs : DEFAULT_ACK_TIMEOUT_MS;
        try {
            // 1) 设备实测协议: iot/cli/<type>/<sn>/eth + {action, serialno, from:"svr", to:<sn>, ...}
            String actionText = buildActionWire(payload, corr, sn);
            if (actionText != null && StringUtils.isNotBlank(accessType)) {
                String topic = String.format(PUB_TOPIC_IOT_TMPL, accessType, sn);
                client.publish(topic, new MqttMessage(actionText.getBytes(StandardCharsets.UTF_8)));
                log.info("[门禁MQTT] 下发 -> {} payload={}", topic, abbreviate(actionText));
            } else {
                // 未实测确认的 action 名一律不猜: 发错命令比不发更危险
                log.warn("[门禁MQTT] cmd={} 无可下发的 action 名, 未向设备下发该命令", cmd);
            }
            // 2) cmd 报文(F1* 标准协议)原样下发。发到哪个主题由 studyroom.access.mqtt.cmd-topic 决定:
            //    cs   = cs/<sn>/csmsg        (文档协议; 设备从 /access/login 取 downTopic 时用它)
            //    eth  = iot/cli/<type>/<sn>/eth (设备从 /server/access 取 mqtt_sub 时用它 —— 本机固件走的正是这条)
            //    both = 两个都发, 谁收到谁执行。多发一条不会造成假成功: 收错协议时设备回的是
            //           {"action":"","data":[{"code":"2"}]}, 既没有 token/serialno 也没有 cmd 名,
            //           匹配不上任何等待方(见 onMessage 的回执分支), 只是白跑一趟。
            if (cmd != null) {
                String text = MAPPER.writeValueAsString(payload);
                for (String t : cmdTopics(sn)) {
                    publishBoth(t, text, cmd, sn);
                }
            }
            return future.get(wait, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            log.warn("[门禁MQTT] 等待设备回执超时({}ms) sn={} cmd={}", wait, sn, cmd);
            return null;
        } catch (Exception e) {
            log.warn("[门禁MQTT] 下发失败 sn={} cmd={}: {}", sn, cmd, e.getMessage());
            return null;
        } finally {
            pending.remove(key(sn, corr));
            if (cmd != null) {
                pending.remove(key(sn, "#" + cmd.toUpperCase()));
            }
        }
    }

    @Override
    public boolean completePending(String sn, String token, Map<String, Object> payload) {
        if (StringUtils.isBlank(sn) || StringUtils.isBlank(token)) {
            return false;
        }
        CompletableFuture<Map<String, Object>> f = pending.get(key(sn, token));
        if (f == null) {
            return false;
        }
        lastSeen.put(sn, System.currentTimeMillis());
        log.info("[门禁MQTT] HTTP 上报回灌, 唤醒等待中的下发 sn={} token={}", sn, token);
        return f.complete(payload);
    }

    /**
     * 本通道能否用服务器指令直接开门 —— 由 {@code studyroom.door.mqtt-open-enabled} 决定, <b>默认 false</b>。
     *
     * <p>2026-10-08 这里曾被改成 {@code true}, 依据是"厂商文档里远程开门走 cs 协议的 cmd 报文"。
     * 那个推理<b>错在前提</b>: 文档描述的是 {@code protocolVersion=1}(标准协议), 而这台设备跑的是
     * {@code protocolVersion=0}(时之盾) —— 设备从未订阅 {@code cs/} 主题, 对我们的指令一律回
     * {@code code:"7"}。完整实测依据见 {@link #mqttOpenEnabled} 字段注释。</p>
     *
     * <p>打开本开关后, 成功判据仍是设备回执 {@code result==0}(见 {@code publish}),
     * 超时/被拒一律如实报失败, 不会出现"提示成功但门没动"。</p>
     */
    @Override
    public boolean supportsRemoteOpen() {
        return mqttOpenEnabled;
    }

    @Override
    public boolean supportsNativeUserSync() {
        return true;
    }

    /**
     * 在设备上建人员 + 下发人脸(设备原生 action 协议, 字段格式取自厂商 broker 真实流量)。
     *
     * <p>实测报文:</p>
     * <pre>
     * 下发 add_user: {"action":"add_user","serialno":"...","from":"svr","to":"&lt;sn&gt;","ts":"...",
     *              "data":[{"user_acct":"...","user_name":"...","auth_mark":"240","auth_group":"0",
     *                       "floor":"FFFFFFFFFFFFFFFF","exp_opt":"0","exp_start":"0","user_type":"32"}]}
     * 设备回执:      {"action":"add_user","data":[{"code":"0","user_acct":"...","user_id":"138"}],...}
     * 下发 add_face: {"action":"add_face","...,"user_id":"138","data":[{"id":"0","type":"jpg","d":"&lt;base64 JPG&gt;"}]}
     * 设备回执:      {"action":"add_face","data":[{"code":"0"}],...}   ← code 0=成功, 1=人脸检测未通过
     * </pre>
     */
    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> enrollUser(String sn, String userAcct, String userName,
                                          String faceBase64Jpeg, Long validUntilSec) {
        if (StringUtils.isBlank(sn) || StringUtils.isBlank(userAcct)) {
            return null;
        }
        long now = System.currentTimeMillis() / 1000;

        // ---- 0) 幂等: 同一账号已存在则复用其 user_id ----
        // add_user 每次调用都会在设备上新建一个人。用户重复上传(换预约/重传照片)会堆出多个同名的人,
        // 而撤销时按账号只会删掉一个, 剩下的仍能刷开设备。所以先按账号查, 已存在就只更新人脸。
        String uid = findExistingUserId(sn, userAcct);
        boolean reusedExisting = StringUtils.isNotBlank(uid);
        if (reusedExisting) {
            log.info("[门禁MQTT] 设备上已存在账号 {}, 复用 user_id={}(只更新人脸)", userAcct, uid);
        } else {
            Map<String, Object> u = new LinkedHashMap<>();
            u.put("user_acct", userAcct);
            u.put("user_name", StringUtils.isBlank(userName) ? userAcct : userName);
            // 权限位取值照抄我方设备上"厂商 App 建出来的普通用户": auth_mark=240 / user_type=32 / 楼层全开
            u.put("auth_mark", "240");
            u.put("auth_group", "0");
            u.put("floor", "FFFFFFFFFFFFFFFF");
            u.put("exp_opt", (validUntilSec == null || validUntilSec <= 0) ? "0" : String.valueOf(validUntilSec));
            u.put("exp_start", "0");
            u.put("user_type", "32");

            Map<String, Object> addUser = new LinkedHashMap<>();
            addUser.put("action", "add_user");
            addUser.put("sn", sn);
            addUser.put("ts", String.valueOf(now));
            addUser.put("data", java.util.Collections.singletonList(u));

            Map<String, Object> r1 = sendAndAwait(sn, addUser, 15_000L);
            uid = firstField(r1, "user_id");
            if (StringUtils.isBlank(uid)) {
                log.warn("[门禁MQTT] 建人员失败 sn={} acct={} ack={}", sn, userAcct,
                    r1 == null ? "无回执" : abbreviate(String.valueOf(r1)));
                return null;
            }
            log.info("[门禁MQTT] 建人员成功 sn={} acct={} user_id={}", sn, userAcct, uid);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("user_id", uid);
        out.put("user_acct", userAcct);

        // ---- 2) 下发人脸 ----
        String pic = sanitizeBase64(faceBase64Jpeg);
        if (StringUtils.isNotBlank(pic)) {
            // 关键: 设备对"已经有人脸的用户"再 add_face 会直接拒绝(实测回 code:"1"),
            // 所以必须先删掉旧脸再下发, 否则用户永远改不了照片、每次上传都是失败。
            // del_face 在厂商流量里验证过: {"action":"del_face","user_id":"139","data":[]} -> code:"0"
            if (reusedExisting) {
                Map<String, Object> delFace = new LinkedHashMap<>();
                delFace.put("action", "del_face");
                delFace.put("sn", sn);
                delFace.put("ts", String.valueOf(System.currentTimeMillis() / 1000));
                delFace.put("user_id", uid);
                delFace.put("data", new java.util.ArrayList<>());
                Map<String, Object> rd = sendAndAwait(sn, delFace, 15_000L);
                log.info("[门禁MQTT] 下发前清理旧人脸 user_id={} 结果={}", uid,
                    rd == null ? "无回执" : str(rd.get("deviceCode")));
            }
            Map<String, Object> f = new LinkedHashMap<>();
            f.put("id", "0");
            f.put("type", "jpg");
            f.put("d", pic);

            Map<String, Object> addFace = new LinkedHashMap<>();
            addFace.put("action", "add_face");
            addFace.put("sn", sn);
            addFace.put("ts", String.valueOf(System.currentTimeMillis() / 1000));
            addFace.put("user_id", uid);
            addFace.put("data", java.util.Collections.singletonList(f));

            Map<String, Object> r2 = sendAndAwait(sn, addFace, 40_000L);
            String code = r2 == null ? null : str(r2.get("deviceCode"));
            out.put("face_ok", "0".equals(code));
            out.put("face_code", code);
            if ("0".equals(code)) {
                log.info("[门禁MQTT] 人脸下发成功 sn={} user_id={} 图片{}KB", sn, uid, pic.length() / 1365);
            } else {
                log.warn("[门禁MQTT] 人脸下发未通过 sn={} user_id={} code={} (1/负数=人脸检测未通过, 详见协议)",
                    sn, uid, code);
            }
        }
        return out;
    }

    @Override
    @SuppressWarnings("unchecked")
    public java.util.List<Map<String, Object>> listDeviceUsers(String sn) {
        java.util.List<Map<String, Object>> all = new java.util.ArrayList<>();
        if (StringUtils.isBlank(sn)) {
            return all;
        }
        for (int page = 1; page <= 10; page++) {
            Map<String, Object> p = new LinkedHashMap<>();
            p.put("action", "read_user");
            p.put("sn", sn);
            p.put("ts", String.valueOf(System.currentTimeMillis() / 1000));
            p.put("user_id", "0");
            p.put("page", String.valueOf(page));
            p.put("size", "100");
            p.put("data", new java.util.ArrayList<>());
            Map<String, Object> r = sendAndAwait(sn, p, 15_000L);
            Object data = r == null ? null : r.get("data");
            if (!(data instanceof java.util.List) || ((java.util.List<?>) data).isEmpty()) {
                break;
            }
            java.util.List<?> list = (java.util.List<?>) data;
            for (Object o : list) {
                if (o instanceof Map) {
                    all.add((Map<String, Object>) o);
                }
            }
            if (list.size() < 100) {
                break;
            }
        }
        return all;
    }

    /** 按账号在设备人员列表里找 user_id(用于幂等复用) */
    private String findExistingUserId(String sn, String userAcct) {
        try {
            for (Map<String, Object> u : listDeviceUsers(sn)) {
                if (userAcct.equals(String.valueOf(u.get("user_acct")))) {
                    return String.valueOf(u.get("user_id"));
                }
            }
        } catch (Exception e) {
            log.warn("[门禁MQTT] 查询设备人员失败(将按新建处理) sn={}: {}", sn, e.getMessage());
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public java.util.List<Map<String, Object>> listDeviceFaces(String sn, int page, int size) {
        java.util.List<Map<String, Object>> all = new java.util.ArrayList<>();
        if (StringUtils.isBlank(sn)) {
            return all;
        }
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("action", "read_face");
        p.put("sn", sn);
        p.put("ts", String.valueOf(System.currentTimeMillis() / 1000));
        p.put("page", String.valueOf(Math.max(1, page)));
        p.put("size", String.valueOf(Math.max(1, Math.min(size, 100))));
        p.put("data", new java.util.ArrayList<>());
        // 实测 size=100 时回包约 536KB, 需要比普通命令更长的等待
        Map<String, Object> r = sendAndAwait(sn, p, 45_000L);
        Object data = r == null ? null : r.get("data");
        if (data instanceof java.util.List) {
            for (Object o : (java.util.List<?>) data) {
                // 失败时只回 [{"code":"5"}], 没有 face_img, 过滤掉
                if (o instanceof Map && ((Map<?, ?>) o).get("face_img") != null) {
                    all.add((Map<String, Object>) o);
                }
            }
        }
        return all;
    }

    @Override
    public boolean deleteDeviceUser(String sn, String deviceUserId) {
        if (StringUtils.isBlank(sn) || StringUtils.isBlank(deviceUserId)) {
            return false;
        }
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("action", "del_user");
        p.put("sn", sn);
        p.put("ts", String.valueOf(System.currentTimeMillis() / 1000));
        p.put("user_id", deviceUserId);
        p.put("data", java.util.Collections.singletonList(
            java.util.Collections.singletonMap("user_id", deviceUserId)));
        Map<String, Object> r = sendAndAwait(sn, p, 15_000L);
        boolean ok = r != null && "0".equals(str(r.get("deviceCode")));
        log.info("[门禁MQTT] 删除人员 sn={} user_id={} -> {}", sn, deviceUserId, ok ? "成功" : "失败");
        return ok;
    }

    /**
     * 只删除人脸, 人员保留。
     * <p>报文与"下发前清理旧人脸"完全一致(厂商 broker 实测 {@code code:"0"} 为成功)。</p>
     */
    @Override
    public boolean deleteDeviceFace(String sn, String deviceUserId) {
        if (StringUtils.isBlank(sn) || StringUtils.isBlank(deviceUserId)) {
            return false;
        }
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("action", "del_face");
        p.put("sn", sn);
        p.put("ts", String.valueOf(System.currentTimeMillis() / 1000));
        p.put("user_id", deviceUserId);
        p.put("data", new java.util.ArrayList<>());
        Map<String, Object> r = sendAndAwait(sn, p, 15_000L);
        String code = r == null ? null : str(r.get("deviceCode"));
        boolean ok = "0".equals(code);
        log.info("[门禁MQTT] 删除人脸(保留人员) sn={} user_id={} code={} -> {}",
            sn, deviceUserId, code, ok ? "成功" : "失败");
        return ok;
    }

    /** 取回执 data[0] 里的字段(设备把 add_user 分配到的 user_id 放在这里) */
    static String firstField(Map<String, Object> ack, String field) {
        if (ack == null) {
            return null;
        }
        Object data = ack.get("data");
        if (data instanceof java.util.List && !((java.util.List<?>) data).isEmpty()) {
            Object o = ((java.util.List<?>) data).get(0);
            if (o instanceof Map) {
                Object v = ((Map<?, ?>) o).get(field);
                if (v != null) {
                    return String.valueOf(v);
                }
            }
        }
        Object v = ack.get(field);
        return v == null ? null : String.valueOf(v);
    }

    /** 人脸数据落地前的清洗: 去掉 data:image/...;base64, 前缀与所有空白字符 */
    static String sanitizeBase64(String s) {
        if (StringUtils.isBlank(s)) {
            return null;
        }
        String t = s.trim();
        int comma = t.indexOf(',');
        if (t.startsWith("data:") && comma > 0) {
            t = t.substring(comma + 1);
        }
        t = t.replaceAll("\\s+", "");
        return t.isEmpty() ? null : t;
    }

    @Override
    public boolean isOnline(String sn) {
        Long seen = lastSeen.get(sn);
        return seen != null && (System.currentTimeMillis() - seen) <= ONLINE_WINDOW_MS;
    }

    @Override
    public Long lastSeenMillis(String sn) {
        return lastSeen.get(sn);
    }

    @Override
    public Set<String> onlineDevices() {
        return Collections.unmodifiableSet(lastSeen.keySet());
    }

    /** 供健康检查/后台展示 */
    public boolean isBrokerConnected() {
        return connected;
    }

    /** 往一个主题发文本, 并把过程写进日志 */
    private void publishBoth(String topic, String text, String cmd, String sn) throws Exception {
        client.publish(topic, new MqttMessage(text.getBytes(StandardCharsets.UTF_8)));
        log.info("[门禁MQTT] 下发 -> {} sn={} cmd={} payload={}", topic, sn, cmd, abbreviate(text));
    }

    /**
     * 组装发往 {@code iot/cli/<type>/<sn>/eth} 的 action 报文。
     *
     * <p>两种来源:</p>
     * <ol>
     *   <li><b>payload 自带 {@code action}</b> —— 直接用它, 并把 {@code user_id/page/size/ts/data}
     *       等顶层字段原样透传。{@code add_user} / {@code add_face} / {@code read_user} /
     *       {@code del_user} / {@code del_face} 走这条, 字段名与取值均照抄厂商 broker 实测报文。</li>
     *   <li><b>payload 只有 {@code cmd}</b> —— 查 {@link #CMD_TO_ACTION} 白名单映射成 action 名。</li>
     * </ol>
     *
     * <p>实测报文形状(注意<b>没有 ts 的指令也存在</b>, 有 ts 的一律用秒级字符串):
     * {@code {"action":"add_user","serialno":"<10位>","from":"svr","to":"<sn>","ts":"<unix秒>",
     * "data":[{...}]}}。</p>
     *
     * @return 可发送的 JSON 文本; 无可用 action 时返回 null(此时绝不发送)
     */
    private static String buildActionWire(Map<String, Object> payload, String serialno, String sn) {
        if (payload == null) {
            return null;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        Object actObj = payload.get("action");
        if (actObj != null && StringUtils.isNotBlank(String.valueOf(actObj))) {
            m.put("action", String.valueOf(actObj));
            // 透传业务字段(user_id / page / size / ts / data ...), 排除仅本地使用的键
            for (Map.Entry<String, Object> e : payload.entrySet()) {
                String k = e.getKey();
                if ("cmd".equals(k) || "token".equals(k) || "sn".equals(k) || "action".equals(k)) {
                    continue;
                }
                if (e.getValue() != null) {
                    m.put(k, e.getValue());
                }
            }
        } else {
            String cmd = str(payload.get("cmd"));
            if (cmd == null) {
                return null;
            }
            String action = CMD_TO_ACTION.get(cmd.toUpperCase());
            if (action == null) {
                return null;
            }
            m.put("action", action);
            m.put("data", payload.get("data") == null ? new java.util.ArrayList<>() : payload.get("data"));
        }
        m.put("serialno", serialno);
        m.put("from", SVR);
        m.put("to", sn);
        if (m.get("data") == null) {
            m.put("data", new java.util.ArrayList<>());
        }
        try {
            return MAPPER.writeValueAsString(m);
        } catch (Exception e) {
            return null;
        }
    }

    private static String key(String sn, String corr) {
        return (sn == null ? "" : sn) + "|" + (corr == null ? "" : corr);
    }

    private static String abbreviate(String s) {
        if (s == null) {
            return "";
        }
        return s.length() > 400 ? s.substring(0, 400) + "...(truncated)" : s;
    }
}