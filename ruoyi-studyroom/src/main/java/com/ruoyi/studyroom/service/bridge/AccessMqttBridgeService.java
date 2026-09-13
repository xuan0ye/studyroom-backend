package com.ruoyi.studyroom.service.bridge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.studyroom.service.IAccessControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessMqttBridgeService {

    private final IAccessControlService accessControlService;
    private final AtomicBoolean redisSubscribed = new AtomicBoolean(false);
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${studyroom.access.bridge.enabled:false}")
    private boolean enabled;
    @Value("${studyroom.access.mqtt.host:}")
    private String mqttHost;
    @Value("${studyroom.access.mqtt.username:}")
    private String mqttUsername;
    @Value("${studyroom.access.mqtt.password:}")
    private String mqttPassword;
    @Value("${studyroom.access.mqtt.pub-topic:face-device/sub}")
    private String mqttPubTopic;
    @Value("${studyroom.access.mqtt.sub-topic:face-device/pub}")
    private String mqttSubTopic;
    @Value("${studyroom.access.redis.command-channel:studyroom:access:command}")
    private String accessCommandChannel;
    @Value("${studyroom.access.bridge.client-id-prefix:studyroom-access-bridge-}")
    private String clientIdPrefix;
    @Value("${studyroom.access.bridge.upstream-callback-url:}")
    private String upstreamCallbackUrl;

    private volatile MqttClient mqttClient;

    @PostConstruct
    public void init() {
        if (!enabled) {
            return;
        }
        subscribeRedisChannelOnce();
        connectAndSubscribe();
    }

    @Scheduled(fixedDelayString = "${studyroom.access.bridge.reconnect-delay-ms:15000}")
    public void keepAlive() {
        if (!enabled) {
            return;
        }
        if (mqttClient == null || !mqttClient.isConnected()) {
            connectAndSubscribe();
        }
    }

    @PreDestroy
    public void shutdown() {
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                mqttClient.disconnect();
            }
        } catch (Exception e) {
            log.warn("access mqtt bridge disconnect failed", e);
        }
    }

    private void subscribeRedisChannelOnce() {
        if (!redisSubscribed.compareAndSet(false, true)) {
            return;
        }
        RedisUtils.subscribe(accessCommandChannel, String.class, this::onRedisCommand);
        log.info("access mqtt bridge subscribed redis channel: {}", accessCommandChannel);
    }

    private synchronized void connectAndSubscribe() {
        if (StringUtils.isBlank(mqttHost)) {
            log.warn("access mqtt bridge mqtt host is empty, skip connect");
            return;
        }
        try {
            if (mqttClient != null && mqttClient.isConnected()) {
                return;
            }
            String clientId = clientIdPrefix + UUID.randomUUID().toString().replace("-", "");
            MqttClient client = new MqttClient(mqttHost, clientId, new MemoryPersistence());
            MqttConnectOptions options = new MqttConnectOptions();
            options.setAutomaticReconnect(true);
            options.setCleanSession(true);
            if (StringUtils.isNotBlank(mqttUsername)) {
                options.setUserName(mqttUsername);
            }
            if (StringUtils.isNotBlank(mqttPassword)) {
                options.setPassword(mqttPassword.toCharArray());
            }
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.warn("access mqtt bridge connection lost", cause);
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    onMqttUpstream(topic, message);
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                }
            });
            client.connect(options);
            client.subscribe(mqttSubTopic, 1);
            mqttClient = client;
            log.info("access mqtt bridge connected, host={}, subTopic={}", mqttHost, mqttSubTopic);
        } catch (Exception e) {
            log.error("access mqtt bridge connect failed", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void onRedisCommand(String rawMessage) {
        if (StringUtils.isBlank(rawMessage)) {
            return;
        }
        try {
            Map<String, Object> message = JsonUtils.parseObject(rawMessage, new TypeReference<Map<String, Object>>() {});
            if (message == null || message.isEmpty()) {
                return;
            }
            Object payloadObj = message.get("payload");
            if (!(payloadObj instanceof Map)) {
                return;
            }
            ensureConnectedBeforePublish();
            if (mqttClient == null || !mqttClient.isConnected()) {
                log.warn("access mqtt bridge not connected, drop command: {}", rawMessage);
                return;
            }
            String payload = JsonUtils.toJsonString(payloadObj);
            MqttMessage mqttMessage = new MqttMessage(payload.getBytes(StandardCharsets.UTF_8));
            mqttMessage.setQos(1);
            mqttClient.publish(mqttPubTopic, mqttMessage);
            log.info("access mqtt bridge published command to topic={}, cmd={}", mqttPubTopic, message.get("cmd"));
        } catch (Exception e) {
            log.error("access mqtt bridge handle redis command error", e);
        }
    }

    private void ensureConnectedBeforePublish() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            connectAndSubscribe();
        }
    }

    private void onMqttUpstream(String topic, MqttMessage message) {
        try {
            String json = new String(message.getPayload(), StandardCharsets.UTF_8);
            Map<String, Object> payload = JsonUtils.parseObject(json, new TypeReference<Map<String, Object>>() {});
            if (payload == null || payload.isEmpty()) {
                return;
            }
            if (StringUtils.isNotBlank(upstreamCallbackUrl)) {
                restTemplate.postForObject(upstreamCallbackUrl, payload, Object.class);
            } else {
                accessControlService.handleMqttDataReport(payload);
            }
            log.info("access mqtt bridge consumed upstream topic={}, cmd={}", topic, payload.get("cmd"));
        } catch (Exception e) {
            log.error("access mqtt bridge handle upstream message error", e);
        }
    }
}
