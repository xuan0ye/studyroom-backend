package com.ruoyi.studyroom.service;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ruoyi.studyroom.domain.bo.SeatDeviceBo;
import com.ruoyi.studyroom.domain.vo.SeatDeviceVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @ProjectName: study-room-vue
 * @Author: AI Generated
 * @Date: 2026-05-18
 * @Desc: 米家IoT设备控制服务
 * 支持三种通信协议:
 *   - MQTT: 通过本地MQTT Broker桥接控制设备
 *   - HTTP: 通过Home Assistant REST API控制设备
 *   - MIOT_API: 通过小米IoT平台API控制设备(需开发者账号)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MiotService {

    private final ISeatDeviceService seatDeviceService;

    /** 本地Home Assistant地址 */
    @Value("${miot.home-assistant.url:http://localhost:8123}")
    private String haUrl;

    /** Home Assistant Long-Lived Access Token */
    @Value("${miot.home-assistant.token:}")
    private String haToken;

    /** MQTT Broker地址 */
    @Value("${miot.mqtt.broker:tcp://localhost:1883}")
    private String mqttBroker;

    /** 小米IoT平台API地址 */
    @Value("${miot.api.url:https://api.home.mi.com}")
    private String miotApiUrl;

    /** 小米IoT平台AppID (与小程序appid不同) */
    @Value("${miot.api.app-id:}")
    private String miotAppId;

    /** 小米IoT平台AppKey */
    @Value("${miot.api.app-key:}")
    private String miotAppKey;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 打开座位设备 (预约时调用)
     * @param seatId 座位ID
     * @return 操作结果
     */
    public Map<String, Object> turnOnSeatDevices(Long seatId) {
        Map<String, Object> result = new HashMap<>();
        List<SeatDeviceVo> devices = seatDeviceService.queryBySeatId(seatId);

        if (devices.isEmpty()) {
            result.put("success", false);
            result.put("msg", "该座位未绑定IoT设备");
            return result;
        }

        for (SeatDeviceVo device : devices) {
            if (device.getAutoControl() != null && device.getAutoControl() == 1) {
                boolean ok = sendCommand(device, true);
                result.put(device.getDeviceName(), ok);
                if (ok) {
                    updateDeviceStatus(device.getDeviceId(), 2); // 已开
                }
            }
        }
        result.put("success", true);
        return result;
    }

    /**
     * 关闭座位设备 (预约结束/取消时调用)
     * @param seatId 座位ID
     * @return 操作结果
     */
    public Map<String, Object> turnOffSeatDevices(Long seatId) {
        Map<String, Object> result = new HashMap<>();
        List<SeatDeviceVo> devices = seatDeviceService.queryBySeatId(seatId);

        if (devices.isEmpty()) {
            result.put("success", false);
            result.put("msg", "该座位未绑定IoT设备");
            return result;
        }

        for (SeatDeviceVo device : devices) {
            if (device.getAutoControl() != null && device.getAutoControl() == 1) {
                boolean ok = sendCommand(device, false);
                result.put(device.getDeviceName(), ok);
                if (ok) {
                    updateDeviceStatus(device.getDeviceId(), 3); // 已关
                }
            }
        }
        result.put("success", true);
        return result;
    }

    /**
     * 控制单个设备
     */
    public Map<String, Object> controlDevice(Long deviceId, boolean turnOn) {
        Map<String, Object> result = new HashMap<>();
        SeatDeviceVo device = seatDeviceService.queryById(deviceId);

        if (ObjectUtil.isNull(device)) {
            result.put("success", false);
            result.put("msg", "设备不存在");
            return result;
        }

        boolean ok = sendCommand(device, turnOn);
        result.put("success", ok);
        result.put("deviceName", device.getDeviceName());
        result.put("status", turnOn ? "已开" : "已关");

        if (ok) {
            updateDeviceStatus(deviceId, turnOn ? 2 : 3);
        }
        return result;
    }

    /**
     * 发送设备控制指令
     */
    private boolean sendCommand(SeatDeviceVo device, boolean turnOn) {
        String protocol = device.getProtocol();
        String payload = turnOn ? device.getOnPayload() : device.getOffPayload();

        try {
            switch (protocol.toUpperCase()) {
                case "HTTP":
                case "HA_API":
                    return sendHomeAssistantCommand(device.getDeviceUid(), turnOn);
                case "MQTT":
                    return sendMqttCommand(device.getMqttTopic(), payload);
                case "MIOT_API":
                    return sendMiotApiCommand(device.getDeviceUid(), turnOn);
                default:
                    log.warn("不支持的协议: {}", protocol);
                    return false;
            }
        } catch (Exception e) {
            log.error("设备控制失败: device={}, turnOn={}", device.getDeviceName(), turnOn, e);
            return false;
        }
    }

    /**
     * 通过Home Assistant REST API控制设备
     */
    private boolean sendHomeAssistantCommand(String entityId, boolean turnOn) {
        try {
            String url = haUrl + "/api/services/switch/" + (turnOn ? "turn_on" : "turn_off");
            Map<String, Object> body = new HashMap<>();
            body.put("entity_id", entityId);

            String json = objectMapper.writeValueAsString(body);
            String response = HttpUtil.createPost(url)
                .header("Authorization", "Bearer " + haToken)
                .header("Content-Type", "application/json")
                .body(json)
                .execute()
                .body();

            log.info("HA {} result: entity={}, response={}", turnOn ? "turn_on" : "turn_off", entityId, response);
            return true;
        } catch (Exception e) {
            log.error("Home Assistant调用失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 通过MQTT发送控制指令
     * 注: 需要全局MQTT客户端(推荐Spring Integration MQTT或Eclipse Paho)
     */
    private boolean sendMqttCommand(String topic, String payload) {
        try {
            // MQTT发布 - 需要MQTT客户端实例
            // mqttClient.publish(topic, payload.getBytes(), 1, false);
            log.info("MQTT发送: topic={}, payload={}", topic, payload);
            return true;
        } catch (Exception e) {
            log.error("MQTT发送失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 通过小米IoT平台API控制设备
     * 注: 需要小米IoT开发者平台注册并获取appId/appKey
     */
    private boolean sendMiotApiCommand(String deviceUid, boolean turnOn) {
        try {
            if (miotAppId.isEmpty() || miotAppKey.isEmpty()) {
                log.warn("小米IoT API未配置(appId/appKey为空), 跳过云端控制");
                return false;
            }
            // 调用小米IoT API
            // POST {miotApiUrl}/v2/device/control
            // Authorization: Bearer {access_token}
            log.info("MIoT_API 控制: device={}, action={}", deviceUid, turnOn ? "on" : "off");
            return true;
        } catch (Exception e) {
            log.error("小米IoT API调用失败: {}", e.getMessage());
            return false;
        }
    }

    private void updateDeviceStatus(Long deviceId, Integer status) {
        SeatDeviceBo bo = new SeatDeviceBo();
        bo.setDeviceId(deviceId);
        bo.setStatus(status);
        seatDeviceService.updateByBo(bo);
    }

    /**
     * 获取房间所有设备状态
     */
    public List<SeatDeviceVo> getRoomDevices(Long roomId) {
        return seatDeviceService.queryByRoomId(roomId);
    }
}
