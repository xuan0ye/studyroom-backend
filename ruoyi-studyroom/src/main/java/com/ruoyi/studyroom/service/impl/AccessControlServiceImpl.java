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
import com.ruoyi.studyroom.service.IDayCardConsumptionService;
import com.ruoyi.studyroom.service.IDeviceChannel;
import com.ruoyi.studyroom.service.ISeatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccessControlServiceImpl implements IAccessControlService {

    private static final String CMD_PREFIX = "F1:";
    /** 标准 websocket 协议的命令前缀: 无冒号(F1heartbeat / F1openDoor / F1upRecord) */
    private static final String WS_CMD_PREFIX = "f1";
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
    /**
     * 门禁设备实时通道(WebSocket 实现)。用 ObjectProvider 延迟获取,
     * 避免与 ruoyi-studyroom-wx 中的实现类形成构造期循环依赖。
     */
    private final ObjectProvider<IDeviceChannel> deviceChannelProvider;
    /**
     * 门禁到店签到的写入方。必须用 ObjectProvider 延迟获取:
     * DayCardConsumptionServiceImpl 反过来依赖 IAccessControlService, 直接构造注入会形成循环依赖。
     */
    private final ObjectProvider<IDayCardConsumptionService> dayCardConsumptionServiceProvider;
    private final RestTemplate restTemplate = new RestTemplate();

    /** 等待设备回执的超时(毫秒) */
    @Value("${studyroom.access.ack-timeout-ms:5000}")
    private long ackTimeoutMs;

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
        // 录脸门槛: 当天有预约, 或未来已有预约(解决"想明天来、今天先录脸"的鸡生蛋问题)
        OrderVo activeOrder = queryCurrentOrFutureOrder(userId);
        if (activeOrder == null) {
            throw new ServiceException("你还没有预约记录，请先预约座位再开通门禁");
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
        // 必须写空串而不是 null: MyBatis-Plus 的 updateById 默认忽略 null 字段,
        // setSyncErrmsg(null) 根本不会更新数据库列, 旧的报错会一直留着 ——
        // 于是"下发成功"之后小程序仍然显示上一次的失败原因(实测踩过)。
        profile.setSyncErrmsg(publishOk ? "" : buildPublishFailReason(device.getSn()));
        saveProfile(profile);
        // 无论下发成败都不在服务器留存人脸照片
        clearStoredFacePhoto(profile);
        AccessProfileVo upVo = toVo(profile);
        enrichDeviceOnline(upVo, device);
        return upVo;
    }

    /**
     * 给档案 VO 补上"门禁设备在线情况"。
     *
     * <p><b>两个接口都必须调</b>: 小程序上传完是把<b>上传接口的返回值</b>直接当档案用的
     * (不会再调一次 /profile/current), 所以上传接口不补的话, 页面会一边显示"人脸已同步到门禁"、
     * 一边显示"门禁设备不在线（从未接入）" —— 用户自然会怀疑是假上传。实测就是这么来的。</p>
     */
    private void enrichDeviceOnline(AccessProfileVo vo, AccessDevice fallbackDevice) {
        if (vo == null) {
            return;
        }
        AccessDevice d = getRoomDevice(vo.getRoomId());
        if (d == null) {
            // 房间→设备这条链没接上时, 退到调用方给的设备, 再退到库里第一台
            d = fallbackDevice != null ? fallbackDevice : firstAccessDevice();
        }
        vo.setDeviceLastOnline(d == null ? null : d.getLastOnlineTime());
        String onlineSn = StringUtils.isNotBlank(vo.getDeviceSn())
            ? vo.getDeviceSn()
            : (d == null ? null : d.getSn());
        if (StringUtils.isBlank(onlineSn)) {
            return;
        }
        // 用进程内的实时活跃时间覆盖库里的值(探针每 60 秒刷新, 比库里的历史值准)
        Long newest = null;
        for (IDeviceChannel c : deviceChannelProvider.orderedStream().collect(java.util.stream.Collectors.toList())) {
            try {
                Long t = c.lastSeenMillis(onlineSn);
                if (t != null && (newest == null || t > newest)) {
                    newest = t;
                }
            } catch (Exception ignored) {
            }
        }
        if (newest != null) {
            vo.setDeviceLastOnline(new Date(newest));
        }
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
        // 能否远程开门只取决于"当天是否有有效预约", 与人脸同步状态无关
        OrderVo activeOrder = queryCurrentActiveOrder(userId, null);
        boolean canOpen = activeOrder != null;
        // 既没上传过人脸、当天也没预约: 维持原有 null 语义(老版本小程序行为不变)
        if (profile == null && !canOpen) {
            return null;
        }
        AccessProfileVo vo = toVo(profile);
        if (vo == null) {
            vo = new AccessProfileVo();
        }
        vo.setCanOpenDoor(canOpen);
        enrichDeviceOnline(vo, null);
        return vo;
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
        // 原始报文只留头 4000 字符: 正常通行记录远小于这个量,
        // 而 read_face 那种回包是几百 KB 的人脸 base64 —— 截断是防止"万一走到这里"时把生物特征整段写进库
        String rawJson = JsonUtils.toJsonString(payload);
        eventLog.setRawPayload(rawJson.length() > 4000 ? rawJson.substring(0, 4000) + "...(truncated)" : rawJson);
        accessEventLogMapper.insert(eventLog);

        if (CMD_UP_RECORD.equals(normalizedCmd) || CMD_UP_TEMP_ABNOR.equals(normalizedCmd)) {
            handlePassEvent(eventLog, sn, normalizedCmd);
        }
        // 门禁刷脸到店 => 签到(取代开灯签到)
        tryDoorCheckIn(payload, sn, cmd);
    }

    /**
     * 门禁刷脸到店 => 签到。
     *
     * <p><b>为什么做字段兼容</b>: 设备推送通行记录的确切字段名我们还没抓到真报文
     * (要等人真的在门口刷一次脸)。所以这里不写死取值路径, 而是把常见命名都试一遍,
     * 并且在认不出身份时<b>把原始字段名打出来</b> —— 这样第一次真实刷脸就能从日志里看出该用哪个字段。</p>
     *
     * <p>识别身份优先用 <code>user_acct</code>(我们自己建的账号是 {@code __sr<工号>}, 可反解),
     * 其次 jobnumber。设备内部 user_id 需要 read_user 反查映射, 暂不在此处做。</p>
     */
    private void tryDoorCheckIn(Map<String, Object> payload, String sn, String cmd) {
        if (payload == null) {
            return;
        }
        // 只看通行类动作: 通行记录推送 / 开门事件。其它(心跳/参数/回复)一律不碰
        String c = cmd == null ? "" : cmd.toLowerCase();
        boolean passLike = c.contains("record") || c.contains("open") || c.contains("pass")
            || c.contains("face") || c.contains("card");
        if (!passLike) {
            return;
        }
        String acct = firstNonBlank(payload, "user_acct", "userAcct", "account");
        Object body = payload.get("body");
        if (acct == null && body instanceof Map) {
            acct = firstNonBlank((Map<String, Object>) body, "user_acct", "userAcct", "account");
        }
        Long userId = null;
        if (acct != null && acct.startsWith("__sr")) {
            try {
                userId = Long.valueOf(acct.substring(4));
            } catch (Exception ignored) {
            }
        }
        if (userId == null) {
            Object job = payload.get("jobnumber");
            if (job == null && body instanceof Map) {
                job = ((Map<String, Object>) body).get("jobnumber");
            }
            if (job != null && StringUtils.isNumeric(String.valueOf(job))) {
                userId = Long.valueOf(String.valueOf(job));
            }
        }
        if (userId == null) {
            // 认不出身份: 不留数据, 但把字段名打出来, 便于第一次真实刷脸后补齐映射
            log.warn("[到店签到] 收到通行类上报但认不出用户 sn={} cmd={} 顶层字段={} body字段={}",
                sn, cmd, payload.keySet(),
                body instanceof Map ? ((Map<String, Object>) body).keySet() : null);
            return;
        }
        Date when = null;
        Object t = payload.get("time");
        if (t == null && body instanceof Map) {
            t = ((Map<String, Object>) body).get("time");
        }
        if (t != null && StringUtils.isNumeric(String.valueOf(t))) {
            long v = Long.parseLong(String.valueOf(t));
            // 秒级时间戳(10 位)转毫秒; 已经是毫秒(13 位)则原样
            when = new Date(v < 100000000000L ? v * 1000L : v);
        }
        try {
            IDayCardConsumptionService svc = dayCardConsumptionServiceProvider.getIfAvailable();
            if (svc != null) {
                svc.checkInByDoor(userId, when);
            }
        } catch (Exception e) {
            log.warn("[到店签到] 写入失败 userId={}: {}", userId, e.getMessage());
        }
    }

    private static String firstNonBlank(Map<String, Object> m, String... keys) {
        for (String k : keys) {
            Object v = m.get(k);
            if (v != null && StringUtils.isNotBlank(String.valueOf(v))) {
                return String.valueOf(v);
            }
        }
        return null;
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
        IDeviceChannel capCh = pickDeviceChannel(device.getSn());
        if (capCh != null && !capCh.supportsRemoteOpen()) {
            // 该型号走厂商 action 协议, 明文通道里没有"远程开门"指令:
            // open_door 只是设备"门已开"的上报事件(且只发给厂商 App), 服务器发同名指令会被拒(实测 code:"1");
            // 真正的远程开门必须由厂商云端签发 {"code":"<不透明串>"} 授权, 密钥在厂商手里。
            // 这里如实拒绝 —— 绝不给用户一个"提示开门成功但门没动"的按钮。
            throw new ServiceException("这台门禁机不支持远程开门（开门授权由厂商云端签发，我们发不出）。"
                + "请到门口刷脸或刷卡开门，或联系工作人员。");
        }
        Map<String, Object> cmd = new HashMap<>();
        cmd.put("cmd", "F1openDoor");
        cmd.put("sn", device.getSn());
        // token: 文档注明"仅远程开机会返回"。设备开门后会把该 token 原样带回
        // 「开门记录上传」(HTTP url/upRecord), 由 AccessRecordController 回灌唤醒本次等待,
        // 于是"门真开了"才有实证 —— 而不是发出去就当成功。
        cmd.put("token", UUID.randomUUID().toString().replace("-", ""));
        boolean ok = publish(device.getSn(), "F1openDoor", cmd);
        if (!ok) {
            // 不能静默 return false: 控制器会包成 R.ok(false), 前端照样提示"开门指令已下发"
            throw new ServiceException(buildPublishFailReason(device.getSn()));
        }
        return true;
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
        boolean publishOk = deleteUserOnDevice(profile, device, orderId);
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
        // ── 优先走设备原生 action 协议 ──────────────────────────────────────────
        // 厂商《云端MQTT协议精简版 v1.12》那套 cmd/F1redactUser 这台固件根本不认
        // (实测下发 F1getUserList 回 {"action":"","data":[{"code":"2"}]} = 未知动作)。
        // 它认的是 add_user(建人员) + add_face(下发人脸), 字段格式已从厂商 broker 真实流量还原,
        // 并在本设备上实测成功(add_user 回 code:"0" 并给出分配的 user_id)。
        IDeviceChannel nativeCh = pickNativeChannel();
        if (nativeCh != null) {
            Map<String, Object> r = nativeCh.enrollUser(device.getSn(), buildUserAcct(profile),
                profile.getUserName(), faceBase64, validityOf(profile));
            if (r != null) {
                boolean faceOk = Boolean.TRUE.equals(r.get("face_ok"));
                boolean hadFace = StringUtils.isNotBlank(faceBase64);
                log.info("[门禁] 原生协议下发人员成功: sn={}, user_id={}, face_code={}, 是否带人脸={}",
                    device.getSn(), r.get("user_id"), r.get("face_code"), hadFace);
                if (hadFace && !faceOk) {
                    // 人员建好了但脸没上 —— 不能报成功, 否则用户以为能刷脸却刷不开门
                    profile.setSyncErrmsg("人员已建，但人脸未通过设备检测(code=" + r.get("face_code") + ")");
                    return false;
                }
                // 顺手更新后台的"设备人员镜像": 我们本来就知道刚写了什么, 没必要为一次录入再去全量重读设备
                try {
                    com.ruoyi.studyroom.controller.StudyroomDeviceAdminController.upsertMirrorUser(
                        device.getSn(), String.valueOf(r.get("user_id")), buildUserAcct(profile), profile.getUserName());
                } catch (Exception e) {
                    log.warn("[门禁] 更新设备人员镜像失败(不影响下发): {}", e.getMessage());
                }
                // 同样用空串清空(见 setSyncErrmsg(null) 会被 MyBatis-Plus 忽略的说明)
                profile.setSyncErrmsg("");
                return true;
            }
            log.warn("[门禁] 原生协议下发失败, 回退旧协议: sn={}", device.getSn());
        }

        // ── 回退: 旧协议(仅对认 cmd 协议的设备有意义) ──────────────────────────
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
        // 标准 websocket 协议的命令名无冒号: F1redactUser。
        // 原写 "F1:redactUser" 既不匹配 WS 的 F1redactUser, 也不匹配 MQTT 的 redactUser。
        payload.put("cmd", "F1redactUser");
        payload.put("sn", device.getSn());
        payload.put("token", profile.getOrderId());
        payload.put("body", body);
        return publish(device.getSn(), "F1redactUser", payload);
    }

    /**
     * 撤销门禁权限: 优先走设备原生 del_user(按账号精确定位), 否则回退旧协议。
     *
     * <p><b>删遍所有同账号记录</b> —— 历史版本每次上传都会新建一个人, 设备上可能残留多个同名账号,
     * 只删一个的话剩下的仍能刷开门。按账号找不到任何人时视为<b>已不存在</b>并返回成功(删除幂等)。</p>
     */
    private boolean deleteUserOnDevice(AccessProfile profile, AccessDevice device, String orderId) {
        IDeviceChannel nativeCh = pickNativeChannel();
        if (nativeCh != null) {
            String acct = buildUserAcct(profile);
            int deleted = 0;
            boolean anyFailed = false;
            for (Map<String, Object> u : nativeCh.listDeviceUsers(device.getSn())) {
                if (!acct.equals(String.valueOf(u.get("user_acct")))) {
                    continue;
                }
                String uid = String.valueOf(u.get("user_id"));
                if (nativeCh.deleteDeviceUser(device.getSn(), uid)) {
                    deleted++;
                } else {
                    anyFailed = true;
                }
            }
            if (deleted > 0) {
                log.info("[门禁] 已删除设备人员 {} 个: sn={} acct={}", deleted, device.getSn(), acct);
                return !anyFailed;
            }
            log.info("[门禁] 设备上未找到账号 {}, 视为已撤销: sn={}", acct, device.getSn());
            return true;
        }
        return publishDeleteUser(profile.getJobnumber(), device, orderId);
    }

    /** 找出支持"设备原生人员管理"的通道(不要求当前在线: 离线时由 sendAndAwait 如实返回失败) */    private IDeviceChannel pickNativeChannel() {
        for (IDeviceChannel c : deviceChannelProvider.orderedStream()
                .collect(java.util.stream.Collectors.toList())) {
            try {
                if (c.supportsNativeUserSync()) {
                    return c;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /**
     * 设备上的人员账号。
     *
     * <p>用固定前缀 + 工号, 保证同一用户在设备上的账号是确定值 —— 这样撤销权限时
     * 可以按账号在设备人员列表里精确定位并删除, <b>无需额外落库设备分配的 user_id</b>。</p>
     */
    private static String buildUserAcct(AccessProfile profile) {
        Object key = profile.getJobnumber() == null ? profile.getId() : profile.getJobnumber();
        return "__sr" + key;
    }

    /**
     * 下发到设备时的人员有效期 —— <b>固定返回 null(即设备端永久有效)</b>。
     *
     * <p>理由: 设备上现有的 128 个用户(厂商 App 建的) {@code exp_opt} <b>全是 "0"(永久)</b>,
     * 厂商的做法就是"设备端永久 + 云端管有效期"。而我们库里 {@code validity} 是预约到期时间戳,
     * 实测 4 条档案<b>全部已过期</b>; 若原样下发, 设备会立刻把该人员当成过期, 用户刷不开门
     * —— 比"多留一点权限"危险得多。撤销权限由 {@link #revokeAccessByOrder} 负责。</p>
     */
    private static Long validityOf(AccessProfile profile) {
        return null;
    }

    private boolean publishDeleteUser(Long jobnumber, AccessDevice device, String token) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("cmd", "F1deleteUser");
        payload.put("sn", device.getSn());
        payload.put("token", token);
        payload.put("jobnumberArray", Arrays.asList(jobnumber));
        return publish(device.getSn(), "F1deleteUser", payload);
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

    /**
     * 放宽后的录脸门槛: <b>当天已有预约, 或未来已有预约</b>, 满足其一即可。
     *
     * <p>原来只认"当天预约", 造成鸡生蛋 —— 用户想明天来、今天先把脸录好, 会被直接拒掉;
     * 而门禁机里已有的 121 张脸大多是从厂商页面录的、不受这条限制, 只有走我们自己小程序的人被挡住。</p>
     *
     * <p><b>但不能把这层校验整个删掉</b>: 那样任何登录用户都能录脸并开通门禁,
     * 等于给所有人发了一把钥匙。门槛必须仍与"确有预约"绑定。</p>
     */
    /** 取库里的第一台门禁设备(当前门店只有一台), 用于房间→设备链没接上时的兜底 */
    private AccessDevice firstAccessDevice() {
        try {
            java.util.List<AccessDevice> list = accessDeviceMapper.selectList(null);
            if (list != null) {
                for (AccessDevice d : list) {
                    if (d != null && StringUtils.isNotBlank(d.getSn())) {
                        return d;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("[门禁] 兜底查询门禁设备失败: {}", e.getMessage());
        }
        return null;
    }

    private OrderVo queryCurrentOrFutureOrder(Long userId) {
        OrderVo today = queryCurrentActiveOrder(userId, null);
        if (today != null || userId == null) {
            return today;
        }
        Date todayStart = cn.hutool.core.date.DateUtil.beginOfDay(new Date());
        LambdaQueryWrapper<DayCardConsumption> lqw = Wrappers.lambdaQuery();
        lqw.eq(DayCardConsumption::getUserId, userId)
            .in(DayCardConsumption::getStatus, 0, 1)
            .ge(DayCardConsumption::getConsumptionDate, todayStart)
            .orderByAsc(DayCardConsumption::getConsumptionDate)
            .last("limit 1");
        DayCardConsumption c = dayCardConsumptionMapper.selectOne(lqw);
        if (c == null) {
            return null;
        }
        SeatVo seat = c.getSeatId() == null ? null : seatService.queryById(c.getSeatId());
        OrderVo vo = new OrderVo();
        vo.setOrderId(String.valueOf(c.getId()));
        vo.setUserId(c.getUserId());
        vo.setSeatId(c.getSeatId());
        vo.setRoomId(seat != null ? seat.getRoomId() : null);
        vo.setStartTime(c.getConsumptionDate());
        return vo;
    }

    private OrderVo queryCurrentActiveOrder(Long userId, Long seatId) {        if (userId == null) {
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
        String lower = cmd.toLowerCase().trim();
        if (lower.startsWith(CMD_PREFIX.toLowerCase())) {
            lower = lower.substring(CMD_PREFIX.length());
        } else if (lower.startsWith(WS_CMD_PREFIX)) {
            // 标准 websocket 协议的命令无冒号(F1heartbeat / F1upRecord)。
            // 旧代码只认 "F1:", 导致设备上报被归一化成 f1uprecord, 永远匹配不上 uprecord 而被丢弃。
            lower = lower.substring(WS_CMD_PREFIX.length());
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

    /**
     * 下发给门禁设备的命令唯一出口。
     *
     * <p>按商家对接文档, 门禁机是<b>主动外连的客户端</b>: 命令必须沿设备已建立的长连接下发,
     * 并等设备回 <code>result</code>/<code>errmsg</code>。这里优先走 {@link IDeviceChannel}(WebSocket 实现):
     * 设备不在线、超时未回、回执 result 非 0 —— 一律返回 false, 由调用方如实告知用户。</p>
     *
     * <p><b>历史坑</b>: 原先这里是 {@code RedisUtils.publish(...); return true;} —— 无条件返回成功。
     * Redis Pub/Sub 在通道无订阅者时静默丢弃消息, 于是"门禁根本没接入"被上报成"已同步/已下发",
     * 前端显示成功而门禁毫无反应(线上 4 条档案全是 sync_status=1, 设备却已离线 48 天)。</p>
     *
     * @return true 仅当设备确实执行成功(result=0)
     */
    private boolean publish(String sn, String cmd, Map<String, Object> payload) {
        IDeviceChannel channel = pickDeviceChannel(sn);
        if (channel != null) {
            if (!channel.isOnline(sn)) {
                log.warn("[门禁] 设备不在线, 命令未下发: sn={}, cmd={}, 当前在线设备={}",
                    sn, cmd, channel.onlineDevices());
                return false;
            }
            Map<String, Object> ack = channel.sendAndAwait(sn, payload, ackTimeoutMs);
            if (ack == null) {
                log.warn("[门禁] 设备未回执(超时或发送失败): sn={}, cmd={}", sn, cmd);
                return false;
            }
            Object result = ack.get("result");
            boolean ok = result == null || "0".equals(String.valueOf(result).trim());
            if (ok) {
                log.info("[门禁] 命令执行成功: sn={}, cmd={}", sn, cmd);
            } else {
                log.warn("[门禁] 设备拒绝执行: sn={}, cmd={}, result={}, errmsg={}",
                    sn, cmd, result, ack.get("errmsg"));
            }
            return ok;
        }
        // 兜底: 没有 WebSocket 通道实现时退回旧的 Redis 广播(仅兼容/排查用)
        try {
            Map<String, Object> event = new HashMap<>();
            event.put("sn", sn);
            event.put("cmd", cmd);
            event.put("payload", payload);
            event.put("sendTime", System.currentTimeMillis());
            long receivers = RedisUtils.publishAndGetReceivers(accessCommandChannel, JsonUtils.toJsonString(event));
            if (receivers <= 0) {
                log.error("门禁命令无人接收(订阅者=0), 消息被 Redis 丢弃: sn={}, cmd={}, channel={}",
                    sn, cmd, accessCommandChannel);
                return false;
            }
            log.info("门禁命令已下发(Redis 广播兜底): sn={}, cmd={}, 接收者={}", sn, cmd, receivers);
            return true;
        } catch (Exception e) {
            log.error("publish access command failed, sn={}, cmd={}", sn, cmd, e);
            return false;
        }
    }

    /**
     * 下发失败时给用户看的原因: 区分"设备从未接入"、"设备长期离线"与"瞬时失败",
     * 避免一律显示笼统的"同步失败"让用户以为重试就能好。
     */
    /**
     * 择优选择设备通道。
     *
     * <p>目前有两套实现: MQTT(厂商《云端MQTT协议精简版 v1.12》, 设备实际在用) 与
     * WebSocket(标准协议, 备选)。<b>不能再用 getIfAvailable()</b> —— 两个 Bean 会抛
     * NoUniqueBeanDefinitionException。这里优先返回"该设备在线"的那条, 否则退回第一条
     * (退回后 sendAndAwait 会因离线返回 null, 调用方如实报失败)。</p>
     */
    private IDeviceChannel pickDeviceChannel(String sn) {
        java.util.List<IDeviceChannel> list = deviceChannelProvider.orderedStream()
            .collect(java.util.stream.Collectors.toList());
        if (list.isEmpty()) {
            return null;
        }
        if (StringUtils.isNotBlank(sn)) {
            // 谁"最近听见过这台设备"就用谁。
            // 不能用 getIfAvailable()(两个 Bean 会抛 NoUniqueBeanDefinitionException),
            // 也不能只取第一个 —— WebSocket 通道会把任意连到 /、/ws 等路径的客户端误登记成设备,
            // 于是它常常"说自己在线"却根本发不出命令, 把真正在线的 MQTT 通道挤掉。
            IDeviceChannel best = null;
            long bestSeen = Long.MIN_VALUE;
            for (IDeviceChannel c : list) {
                try {
                    if (!c.isOnline(sn)) {
                        continue;
                    }
                    Long seen = c.lastSeenMillis(sn);
                    long v = seen == null ? 0L : seen;
                    if (best == null || v > bestSeen) {
                        best = c;
                        bestSeen = v;
                    }
                } catch (Exception ignored) {
                }
            }
            if (best != null) {
                return best;
            }
        }
        // 都不在线: 退回第一个(约定 MQTT 通道优先), 让 sendAndAwait 如实返回 null
        return list.get(0);
    }

    private String buildPublishFailReason(String sn) {
        // 优先看进程内的实时连接状态: 比数据库 last_online_time 更及时
        IDeviceChannel channel = pickDeviceChannel(sn);
        if (channel != null && StringUtils.isNotBlank(sn)) {
            Long seen = channel.lastSeenMillis(sn);
            if (!channel.isOnline(sn) && seen != null) {
                return "门禁设备连接已断开（最后活跃 "
                    + cn.hutool.core.date.DateUtil.format(new Date(seen), "MM-dd HH:mm")
                    + "），指令未送达。请到店联系工作人员开门。";
            }
        }
        AccessDevice device = StringUtils.isNotBlank(sn) ? getBySn(sn) : null;
        if (device == null || device.getLastOnlineTime() == null) {
            return "门禁设备尚未接入系统，指令无法送达。请到店联系工作人员开门。";
        }
        long days = (System.currentTimeMillis() - device.getLastOnlineTime().getTime()) / 86400000L;
        if (days >= 1) {
            return "门禁设备已离线 " + days + " 天（最后在线 "
                + cn.hutool.core.date.DateUtil.format(device.getLastOnlineTime(), "yyyy-MM-dd HH:mm")
                + "），指令未送达。请到店联系工作人员开门。";
        }
        return "门禁设备当前无响应，指令未送达，请稍后重试或联系工作人员。";
    }
}
