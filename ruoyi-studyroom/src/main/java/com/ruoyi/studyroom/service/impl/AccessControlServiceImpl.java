package com.ruoyi.studyroom.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.DateUtils;
import com.ruoyi.common.utils.JsonUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.studyroom.domain.AccessDevice;
import com.ruoyi.studyroom.domain.AccessEventLog;
import com.ruoyi.studyroom.domain.AccessProfile;
import com.ruoyi.studyroom.domain.DayCardConsumption;
import com.ruoyi.studyroom.domain.bo.AccessDeviceLoginBo;
import com.ruoyi.studyroom.domain.bo.AccessFaceUploadBo;
import com.ruoyi.studyroom.domain.vo.AccessProfileVo;
import com.ruoyi.studyroom.domain.vo.OrderVo;
import com.ruoyi.studyroom.domain.vo.SeatVo;
import com.ruoyi.studyroom.mapper.AccessDeviceMapper;
import com.ruoyi.studyroom.mapper.AccessEventLogMapper;
import com.ruoyi.studyroom.mapper.AccessProfileMapper;
import com.ruoyi.studyroom.mapper.DayCardConsumptionMapper;
import com.ruoyi.studyroom.service.IAccessControlService;
import com.ruoyi.studyroom.service.ISeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessControlServiceImpl implements IAccessControlService {

    private static final String CMD_PREFIX = "F1:";
    private static final String CMD_HEARTBEAT = "heartbeat";
    private static final String CMD_UP_RECORD = "uprecord";
    private static final String CMD_UP_TEMP_ABNOR = "uptempabnorinfo";
    private static final int REVOKE_STATUS_NONE = 0;
    private static final int REVOKE_STATUS_DONE = 1;
    private static final int REVOKE_STATUS_WAIT_RETRY = 2;

    private final DayCardConsumptionMapper dayCardConsumptionMapper;
    private final ISeatService seatService;
    private final AccessDeviceMapper accessDeviceMapper;
    private final AccessProfileMapper accessProfileMapper;
    private final AccessEventLogMapper accessEventLogMapper;
    private final RestTemplate restTemplate = new RestTemplate();

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
    @Value("${studyroom.access.alert.webhook-url:}")
    private String accessAlertWebhookUrl;
    @Value("${studyroom.access.alert.temperature-threshold:37.3}")
    private BigDecimal temperatureThreshold;
    @Value("${studyroom.access.revoke.max-retry-attempts:5}")
    private int revokeMaxRetryAttempts;
    @Value("${studyroom.access.revoke.backoff-minutes:1,3,10}")
    private String revokeBackoffMinutes;

    @Override
    public AccessProfileVo uploadFaceAndSync(Long userId, String userName, AccessFaceUploadBo bo) {
        if (bo == null) {
            throw new ServiceException("请求参数不能为空");
        }
        if (StringUtils.isBlank(bo.getFacePhotoBase64()) && StringUtils.isBlank(bo.getFacePhotoUrl())) {
            throw new ServiceException("请上传门禁照片");
        }
        String base64 = normalizeBase64(bo.getFacePhotoBase64());
        if (StringUtils.isNotBlank(base64) && base64.length() > 300000) {
            throw new ServiceException("照片过大，请压缩后重试");
        }
        OrderVo activeOrder = queryCurrentActiveOrder(userId, null);
        if (activeOrder == null) {
            throw new ServiceException("当前无有效预约，无法开通门禁");
        }
        Long roomId = bo.getRoomId() == null ? activeOrder.getRoomId() : bo.getRoomId();
        if (roomId != null && activeOrder.getRoomId() != null && !roomId.equals(activeOrder.getRoomId())) {
            throw new ServiceException("仅允许给当前预约自习室上传门禁照片");
        }
        AccessProfile profile = getOrCreateProfile(userId, activeOrder.getOrderId());
        profile.setUserId(userId);
        profile.setRoomId(activeOrder.getRoomId());
        profile.setSeatId(activeOrder.getSeatId());
        profile.setOrderId(activeOrder.getOrderId());
        profile.setJobnumber(userId);
        profile.setUserName(StringUtils.defaultIfBlank(userName, "用户" + userId));
        // ===== 隐私最小化: 人脸照片不写入后端数据库 =====
        // 人脸照片仅在内存中一次性下发到门禁设备本地存储(仅用于刷脸比对开门),
        // 后端 sr_access_profile 不保存 face_photo_base64 / face_photo_url
        profile.setFacePhotoBase64(null);
        profile.setFacePhotoUrl(null);
        profile.setRecordPic(1);
        profile.setValidity(activeOrder.getEndTime() == null ? 0L : activeOrder.getEndTime().getTime() / 1000);
        profile.setSyncStatus(0);
        profile.setSyncErrmsg(null);
        profile.setRevokeStatus(REVOKE_STATUS_NONE);
        profile.setRevokeRetryCount(0);
        profile.setNextRevokeRetryTime(null);

        AccessDevice device = getRoomDevice(profile.getRoomId());
        if (device == null) {
            profile.setSyncStatus(2);
            profile.setSyncErrmsg("当前自习室未绑定门禁设备");
            saveProfile(profile);
            clearStoredFacePhoto(profile);
            return toVo(profile);
        }

        profile.setDeviceSn(device.getSn());
        boolean publishOk = publishRedactUser(profile, device, base64);
        profile.setSyncStatus(publishOk ? 1 : 2);
        profile.setSyncErrmsg(publishOk ? null : "门禁命令下发失败");
        saveProfile(profile);
        // 无论下发成败都不在服务器留存人脸照片
        clearStoredFacePhoto(profile);
        return toVo(profile);
    }

    /** 清空历史遗留/本次误写的人脸照片列(隐私: 服务器不存储人脸) */
    private void clearStoredFacePhoto(AccessProfile profile) {
        try {
            if (profile != null && profile.getId() != null) {
                accessProfileMapper.update(null, Wrappers.<AccessProfile>lambdaUpdate()
                    .eq(AccessProfile::getId, profile.getId())
                    .set(AccessProfile::getFacePhotoBase64, null)
                    .set(AccessProfile::getFacePhotoUrl, null));
            }
        } catch (Exception e) {
            log.warn("清理服务器人脸照片失败 profileId={}", profile == null ? null : profile.getId(), e);
        }
    }

    @Override
    public AccessProfileVo queryCurrentProfile(Long userId, Long roomId) {
        LambdaQueryWrapper<AccessProfile> lqw = Wrappers.lambdaQuery();
        lqw.eq(AccessProfile::getUserId, userId)
            .eq(roomId != null, AccessProfile::getRoomId, roomId)
            .orderByDesc(AccessProfile::getUpdateTime)
            .last("limit 1");
        AccessProfile profile = accessProfileMapper.selectOne(lqw);
        return toVo(profile);
    }

    @Override
    public Map<String, Object> buildDeviceLoginResponse(AccessDeviceLoginBo bo) {
        if (bo == null || StringUtils.isBlank(bo.getSn())) {
            throw new ServiceException("sn不能为空");
        }
        upsertDeviceOnLogin(bo);
        Map<String, Object> mqInfo = new HashMap<>();
        mqInfo.put("host", mqttHost);
        mqInfo.put("username", mqttUsername);
        mqInfo.put("password", mqttPassword);
        mqInfo.put("pubTopic", mqttPubTopic);
        mqInfo.put("subTopic", mqttSubTopic);
        mqInfo.put("clientId", "face-device-" + bo.getSn());

        Map<String, Object> data = new HashMap<>();
        data.put("sn", bo.getSn());
        data.put("mqinfo", mqInfo);

        Map<String, Object> resp = new HashMap<>();
        resp.put("code", 0);
        resp.put("msg", "success");
        resp.put("data", data);
        return resp;
    }

    @Override
    @SuppressWarnings("unchecked")
    public void handleMqttDataReport(Map<String, Object> payload) {
        if (payload == null || payload.isEmpty()) {
            return;
        }
        String cmd = String.valueOf(payload.getOrDefault("cmd", ""));
        String sn = String.valueOf(payload.getOrDefault("sn", ""));
        if (StringUtils.isBlank(sn)) {
            return;
        }
        String normalizedCmd = normalizeCmd(cmd);
        if (CMD_HEARTBEAT.equals(normalizedCmd)) {
            AccessDevice device = getBySn(sn);
            if (device != null) {
                device.setStatus(1);
                device.setLastOnlineTime(new Date());
                accessDeviceMapper.updateById(device);
            }
            return;
        }

        Map<String, Object> body = null;
        Object bodyObj = payload.get("body");
        if (bodyObj instanceof Map) {
            body = (Map<String, Object>) bodyObj;
        }
        AccessEventLog eventLog = new AccessEventLog();
        eventLog.setSn(sn);
        eventLog.setCmd(cmd);
        if (body != null) {
            Object job = body.get("jobnumber");
            if (job != null && StringUtils.isNumeric(String.valueOf(job))) {
                eventLog.setJobnumber(Long.valueOf(String.valueOf(job)));
            }
            eventLog.setUserName(String.valueOf(body.getOrDefault("name", body.getOrDefault("userName", ""))));
            Object timeObj = body.get("time");
            if (timeObj != null && StringUtils.isNumeric(String.valueOf(timeObj))) {
                eventLog.setEventTime(new Date(Long.parseLong(String.valueOf(timeObj)) * 1000));
            } else {
                eventLog.setEventTime(new Date());
            }
            Object tempObj = body.get("temperature");
            if (tempObj != null && StringUtils.isNotBlank(String.valueOf(tempObj))) {
                try {
                    eventLog.setTemperature(new BigDecimal(String.valueOf(tempObj)));
                } catch (Exception ignored) {
                }
            }
            // 通行事件只记录元数据(时间/工号/温度), 不落库抓拍图片(隐私最小化, 人脸影像仅在门禁本地)
            // (原 setPicBase64 已移除: pic_base64 列不再写入)
        } else {
            eventLog.setEventTime(new Date());
        }
        eventLog.setRawPayload(JsonUtils.toJsonString(payload));
        accessEventLogMapper.insert(eventLog);

        if (CMD_UP_RECORD.equals(normalizedCmd) || CMD_UP_TEMP_ABNOR.equals(normalizedCmd)) {
            handlePassEvent(eventLog, sn, normalizedCmd);
        }
    }

    @Override
    public boolean remoteOpenDoor(Long userId, Long roomId) {
        OrderVo activeOrder = queryCurrentActiveOrder(userId, null);
        if (activeOrder == null) {
            throw new ServiceException("当前无有效预约，不能远程开门");
        }
        if (roomId != null && activeOrder.getRoomId() != null && !roomId.equals(activeOrder.getRoomId())) {
            throw new ServiceException("仅允许开启当前预约自习室门禁");
        }
        AccessDevice device = getRoomDevice(activeOrder.getRoomId());
        if (device == null) {
            throw new ServiceException("当前自习室未配置门禁设备");
        }
        Map<String, Object> cmd = new HashMap<>();
        cmd.put("cmd", "F1:openDoor");
        cmd.put("sn", device.getSn());
        return publish(device.getSn(), "F1:openDoor", cmd);
    }

    @Override
    public void revokeAccessByOrder(String orderId, Long userId, Long roomId) {
        AccessProfile profile = findProfileForRevoke(orderId, userId, roomId);
        if (profile == null || profile.getJobnumber() == null) {
            return;
        }
        if (Integer.valueOf(REVOKE_STATUS_DONE).equals(profile.getRevokeStatus())) {
            return;
        }
        AccessDevice device = StringUtils.isNotBlank(profile.getDeviceSn()) ? getBySn(profile.getDeviceSn()) : null;
        if (device == null) {
            device = getRoomDevice(profile.getRoomId() != null ? profile.getRoomId() : roomId);
        }
        if (device == null) {
            markRevokeFailed(profile, "预约结束，但未找到门禁设备");
            return;
        }
        boolean publishOk = publishDeleteUser(profile.getJobnumber(), device, orderId);
        if (publishOk) {
            profile.setSyncErrmsg("预约结束，已下发删除门禁权限");
            profile.setRevokeStatus(REVOKE_STATUS_DONE);
            profile.setRevokeRetryCount(0);
            profile.setNextRevokeRetryTime(null);
        } else {
            markRevokeFailed(profile, "预约结束，但删除门禁权限下发失败");
        }
        profile.setUpdateTime(DateUtils.getNowDate());
        accessProfileMapper.updateById(profile);
    }

    @Override
    public int compensateFailedRevokes() {
        Date now = new Date();
        LambdaQueryWrapper<AccessProfile> lqw = Wrappers.lambdaQuery();
        lqw.eq(AccessProfile::getRevokeStatus, REVOKE_STATUS_WAIT_RETRY)
            .and(w -> w.isNull(AccessProfile::getNextRevokeRetryTime).or().le(AccessProfile::getNextRevokeRetryTime, now))
            .orderByAsc(AccessProfile::getNextRevokeRetryTime)
            .last("limit 100");
        int affected = 0;
        for (AccessProfile profile : accessProfileMapper.selectList(lqw)) {
            if (profile.getJobnumber() == null) {
                continue;
            }
            if (profile.getRevokeRetryCount() != null && profile.getRevokeRetryCount() >= revokeMaxRetryAttempts) {
                continue;
            }
            AccessDevice device = StringUtils.isNotBlank(profile.getDeviceSn()) ? getBySn(profile.getDeviceSn()) : null;
            if (device == null) {
                device = getRoomDevice(profile.getRoomId());
            }
            if (device == null) {
                markRevokeFailed(profile, "补偿重试失败：未找到门禁设备");
                accessProfileMapper.updateById(profile);
                continue;
            }
            boolean ok = publishDeleteUser(profile.getJobnumber(), device, profile.getOrderId());
            if (ok) {
                profile.setRevokeStatus(REVOKE_STATUS_DONE);
                profile.setRevokeRetryCount(0);
                profile.setNextRevokeRetryTime(null);
                profile.setSyncErrmsg("门禁撤销补偿成功");
            } else {
                markRevokeFailed(profile, "门禁撤销补偿失败");
            }
            profile.setUpdateTime(DateUtils.getNowDate());
            accessProfileMapper.updateById(profile);
            affected++;
        }
        return affected;
    }

    private String normalizeBase64(String input) {
        if (StringUtils.isBlank(input)) {
            return input;
        }
        int idx = input.indexOf("base64,");
        return idx > -1 ? input.substring(idx + 7) : input;
    }

    private AccessProfile getOrCreateProfile(Long userId, String orderId) {
        LambdaQueryWrapper<AccessProfile> lqw = Wrappers.lambdaQuery();
        lqw.eq(AccessProfile::getUserId, userId)
            .eq(StringUtils.isNotBlank(orderId), AccessProfile::getOrderId, orderId)
            .orderByDesc(AccessProfile::getUpdateTime)
            .last("limit 1");
        AccessProfile profile = accessProfileMapper.selectOne(lqw);
        return profile == null ? new AccessProfile() : profile;
    }

    private void saveProfile(AccessProfile profile) {
        profile.setUpdateTime(DateUtils.getNowDate());
        if (profile.getId() == null) {
            profile.setCreateTime(DateUtils.getNowDate());
            accessProfileMapper.insert(profile);
        } else {
            accessProfileMapper.updateById(profile);
        }
    }

    private AccessProfileVo toVo(AccessProfile profile) {
        if (profile == null) {
            return null;
        }
        AccessProfileVo vo = new AccessProfileVo();
        vo.setOrderId(profile.getOrderId());
        vo.setRoomId(profile.getRoomId());
        vo.setSeatId(profile.getSeatId());
        vo.setDeviceSn(profile.getDeviceSn());
        vo.setJobnumber(profile.getJobnumber());
        vo.setSyncStatus(profile.getSyncStatus());
        vo.setSyncErrmsg(profile.getSyncErrmsg());
        vo.setValidity(profile.getValidity());
        vo.setFacePhotoUrl(profile.getFacePhotoUrl());
        return vo;
    }

    private AccessDevice getRoomDevice(Long roomId) {
        if (roomId == null) {
            return null;
        }
        LambdaQueryWrapper<AccessDevice> lqw = Wrappers.lambdaQuery();
        lqw.eq(AccessDevice::getRoomId, roomId)
            .orderByDesc(AccessDevice::getLastOnlineTime)
            .last("limit 1");
        return accessDeviceMapper.selectOne(lqw);
    }

    private AccessDevice getBySn(String sn) {
        LambdaQueryWrapper<AccessDevice> lqw = Wrappers.lambdaQuery();
        lqw.eq(AccessDevice::getSn, sn).last("limit 1");
        return accessDeviceMapper.selectOne(lqw);
    }

    private void upsertDeviceOnLogin(AccessDeviceLoginBo bo) {
        AccessDevice exist = getBySn(bo.getSn());
        if (exist == null) {
            exist = new AccessDevice();
            exist.setSn(bo.getSn());
            exist.setCreateTime(new Date());
        }
        exist.setRegisterNo(bo.getRegisterNo());
        exist.setDeviceName(bo.getName());
        exist.setDeviceType(bo.getType());
        exist.setDeviceIp(bo.getDeviceIp());
        exist.setStatus(1);
        exist.setLastOnlineTime(new Date());
        exist.setUpdateTime(new Date());
        if (exist.getId() == null) {
            accessDeviceMapper.insert(exist);
        } else {
            accessDeviceMapper.updateById(exist);
        }
    }

    private boolean publishRedactUser(AccessProfile profile, AccessDevice device, String faceBase64) {
        Map<String, Object> body = new HashMap<>();
        body.put("jobnumber", profile.getJobnumber());
        body.put("sTime", System.currentTimeMillis() / 1000);
        body.put("eTime", System.currentTimeMillis() / 1000);
        body.put("userName", profile.getUserName());
        body.put("userType", 2);
        body.put("recordPic", profile.getRecordPic() == null ? 1 : profile.getRecordPic());
        // 人脸照片仅作为下发参数(内存), 不经过 profile 字段落库
        body.put("pic", faceBase64 == null ? "" : faceBase64);
        body.put("validity", profile.getValidity() == null ? 0 : profile.getValidity());

        Map<String, Object> payload = new HashMap<>();
        payload.put("cmd", "F1:redactUser");
        payload.put("sn", device.getSn());
        payload.put("token", profile.getOrderId());
        payload.put("body", body);
        return publish(device.getSn(), "F1:redactUser", payload);
    }

    private boolean publishDeleteUser(Long jobnumber, AccessDevice device, String token) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("cmd", "F1:deleteUser");
        payload.put("sn", device.getSn());
        payload.put("token", token);
        payload.put("jobnumberArray", Arrays.asList(jobnumber));
        return publish(device.getSn(), "F1:deleteUser", payload);
    }

    private void markRevokeFailed(AccessProfile profile, String message) {
        int retries = profile.getRevokeRetryCount() == null ? 0 : profile.getRevokeRetryCount();
        retries++;
        profile.setRevokeStatus(REVOKE_STATUS_WAIT_RETRY);
        profile.setRevokeRetryCount(retries);
        profile.setNextRevokeRetryTime(calculateNextRetryTime(retries));
        profile.setSyncErrmsg(message);
    }

    private Date calculateNextRetryTime(int retryCount) {
        String[] parts = StringUtils.split(revokeBackoffMinutes, ',');
        int idx = Math.max(0, Math.min(retryCount - 1, (parts == null ? 0 : parts.length - 1)));
        int minutes = 1;
        if (parts != null && parts.length > 0) {
            try {
                minutes = Integer.parseInt(parts[idx].trim());
            } catch (Exception ignored) {
            }
        }
        return new Date(System.currentTimeMillis() + minutes * 60_000L);
    }

    private AccessProfile findProfileForRevoke(String orderId, Long userId, Long roomId) {
        LambdaQueryWrapper<AccessProfile> byOrder = Wrappers.lambdaQuery();
        byOrder.eq(StringUtils.isNotBlank(orderId), AccessProfile::getOrderId, orderId)
            .orderByDesc(AccessProfile::getUpdateTime)
            .last("limit 1");
        AccessProfile profile = StringUtils.isNotBlank(orderId) ? accessProfileMapper.selectOne(byOrder) : null;
        if (profile != null) {
            return profile;
        }
        if (userId == null) {
            return null;
        }
        LambdaQueryWrapper<AccessProfile> lqw = Wrappers.lambdaQuery();
        lqw.eq(AccessProfile::getUserId, userId)
            .eq(roomId != null, AccessProfile::getRoomId, roomId)
            .orderByDesc(AccessProfile::getUpdateTime)
            .last("limit 1");
        return accessProfileMapper.selectOne(lqw);
    }

    private OrderVo queryCurrentActiveOrder(Long userId, Long seatId) {
        if (userId == null) {
            return null;
        }
        // 日卡/周卡预约体系: 查今天的消费记录(0待使用 或 1已使用; 已使用=已到店开灯, 门禁/远程开门仍有效)
        Date today = cn.hutool.core.date.DateUtil.beginOfDay(new Date());
        Date tomorrow = cn.hutool.core.date.DateUtil.offsetDay(today, 1);
        LambdaQueryWrapper<DayCardConsumption> lqw = Wrappers.lambdaQuery();
        lqw.eq(DayCardConsumption::getUserId, userId)
            .in(DayCardConsumption::getStatus, 0, 1)
            .ge(DayCardConsumption::getConsumptionDate, today)
            .lt(DayCardConsumption::getConsumptionDate, tomorrow)
            .eq(seatId != null, DayCardConsumption::getSeatId, seatId)
            .orderByDesc(DayCardConsumption::getCreateTime)
            .last("limit 1");
        DayCardConsumption consumption = dayCardConsumptionMapper.selectOne(lqw);
        if (consumption == null) {
            return null;
        }
        SeatVo seat = consumption.getSeatId() == null ? null : seatService.queryById(consumption.getSeatId());
        OrderVo vo = new OrderVo();
        vo.setOrderId(String.valueOf(consumption.getId()));
        vo.setUserId(consumption.getUserId());
        vo.setSeatId(consumption.getSeatId());
        vo.setRoomId(seat != null ? seat.getRoomId() : null);
        vo.setStartTime(consumption.getConsumptionDate());
        vo.setEndTime(tomorrow);
        vo.setPayStatus(3); // NOT_CONSUMED
        vo.setOrderStatus("ACTIVE");
        return vo;
    }

    private String normalizeCmd(String cmd) {
        if (StringUtils.isBlank(cmd)) {
            return "";
        }
        String lower = cmd.toLowerCase();
        if (lower.startsWith(CMD_PREFIX.toLowerCase())) {
            lower = lower.substring(CMD_PREFIX.length());
        }
        return lower.replace(":", "");
    }

    private void handlePassEvent(AccessEventLog eventLog, String sn, String cmd) {
        Long userId = eventLog.getJobnumber();
        if (userId == null || userId <= 0) {
            return;
        }
        AccessDevice device = getBySn(sn);
        Long roomId = device == null ? null : device.getRoomId();
        OrderVo activeOrder = queryCurrentActiveOrder(userId, null);
        if (activeOrder == null || (roomId != null && activeOrder.getRoomId() != null && !roomId.equals(activeOrder.getRoomId()))) {
            log.warn("检测到无效门禁通行 userId={}, sn={}, roomId={}, cmd={}", userId, sn, roomId, cmd);
            revokeAccessByOrder(null, userId, roomId);
            pushAccessAlert("检测到无效门禁通行", eventLog, roomId);
            return;
        }
        if (eventLog.getTemperature() != null && temperatureThreshold != null
            && eventLog.getTemperature().compareTo(temperatureThreshold) >= 0) {
            pushAccessAlert("检测到高温通行", eventLog, roomId);
        }
    }

    private void pushAccessAlert(String title, AccessEventLog eventLog, Long roomId) {
        if (StringUtils.isBlank(accessAlertWebhookUrl)) {
            return;
        }
        Map<String, Object> alert = new HashMap<>();
        alert.put("title", title);
        alert.put("sn", eventLog.getSn());
        alert.put("cmd", eventLog.getCmd());
        alert.put("jobnumber", eventLog.getJobnumber());
        alert.put("userName", eventLog.getUserName());
        alert.put("roomId", roomId);
        alert.put("temperature", eventLog.getTemperature());
        alert.put("eventTime", eventLog.getEventTime());
        try {
            restTemplate.postForObject(accessAlertWebhookUrl, alert, Object.class);
        } catch (Exception e) {
            log.warn("push access alert failed", e);
        }
    }

    private boolean publish(String sn, String cmd, Map<String, Object> payload) {
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("sn", sn);
            event.put("cmd", cmd);
            event.put("payload", payload);
            event.put("sendTime", System.currentTimeMillis());
            RedisUtils.publish(accessCommandChannel, JsonUtils.toJsonString(event));
            return true;
        } catch (Exception e) {
            log.error("publish access command failed, sn={}, cmd={}", sn, cmd, e);
            return false;
        }
    }
}
