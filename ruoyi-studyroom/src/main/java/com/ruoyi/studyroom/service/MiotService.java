package com.ruoyi.studyroom.service;

import cn.hutool.core.util.ObjectUtil;
import com.ruoyi.studyroom.domain.bo.SeatDeviceBo;
import com.ruoyi.studyroom.domain.vo.SeatDeviceVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class MiotService {

    private final ISeatDeviceService seatDeviceService;

    /** 米家桥接脚本路径, 可配置(默认本机开发路径; 服务器部署时通过配置覆盖) */
    @Value("${studyroom.miot.bridge-script:G:\\1自习室小程序\\tools\\miot_bridge.py}")
    private String bridgeScript;

    /** 调用桥接脚本的 Python 可执行文件(默认 python, 服务器可用绝对路径) */
    @Value("${studyroom.miot.python:python}")
    private String pythonExec;

    public Map<String, Object> turnOnSeatDevices(Long seatId) {
        Map<String, Object> result = new HashMap<>();
        List<SeatDeviceVo> devices = seatDeviceService.queryBySeatId(seatId);
        if (devices.isEmpty()) {
            result.put("success", false);
            result.put("msg", "该座位未绑定开关设备");
            return result;
        }
        int controlled = 0;
        int sent = 0;
        for (SeatDeviceVo device : devices) {
            if (device.getAutoControl() != null && device.getAutoControl() == 1) {
                controlled++;
                boolean ok = sendCommand(device, true);
                result.put(device.getDeviceName(), ok);
                if (ok) {
                    sent++;
                    updateDeviceStatus(device.getDeviceId(), 2);
                }
            }
        }
        fillSuccess(result, controlled, sent, "开灯");
        return result;
    }

    public Map<String, Object> turnOffSeatDevices(Long seatId) {
        Map<String, Object> result = new HashMap<>();
        List<SeatDeviceVo> devices = seatDeviceService.queryBySeatId(seatId);
        if (devices.isEmpty()) {
            result.put("success", false);
            result.put("msg", "该座位未绑定开关设备");
            return result;
        }
        int controlled = 0;
        int sent = 0;
        for (SeatDeviceVo device : devices) {
            if (device.getAutoControl() != null && device.getAutoControl() == 1) {
                controlled++;
                boolean ok = sendCommand(device, false);
                result.put(device.getDeviceName(), ok);
                if (ok) {
                    sent++;
                    updateDeviceStatus(device.getDeviceId(), 3);
                }
            }
        }
        fillSuccess(result, controlled, sent, "关灯");
        return result;
    }

    /**
     * 如实写入 success: 只有确实向设备下发成功才算成功。
     * 历史实现无论设备下发成功与否都返回 success=true，前端会误报"灯已开启"。
     */
    private void fillSuccess(Map<String, Object> result, int controlled, int sent, String action) {
        if (controlled == 0) {
            result.put("success", false);
            result.put("msg", "该座位设备未开启自动控制");
        } else if (sent == 0) {
            result.put("success", false);
            result.put("msg", action + "指令下发失败，请稍后重试");
        } else {
            result.put("success", true);
        }
    }

    public Map<String, Object> controlDevice(Long deviceId, boolean turnOn) {
        Map<String, Object> result = new HashMap<>();
        SeatDeviceVo device = seatDeviceService.queryById(deviceId);
        if (ObjectUtil.isNull(device)) {
            result.put("success", false);
            result.put("msg", "Device not found");
            return result;
        }
        boolean ok = sendCommand(device, turnOn);
        result.put("success", ok);
        result.put("deviceName", device.getDeviceName());
        result.put("status", turnOn ? "on" : "off");
        if (ok) updateDeviceStatus(deviceId, turnOn ? 2 : 3);
        return result;
    }

    /**
     * 只读查询设备当前开关状态(后台「灯控自检」用, 不改变设备)
     */
    public Map<String, Object> queryDeviceStatus(Long deviceId) {
        Map<String, Object> result = new HashMap<>();
        SeatDeviceVo device = seatDeviceService.queryById(deviceId);
        if (ObjectUtil.isNull(device)) {
            result.put("success", false);
            result.put("msg", "设备不存在");
            return result;
        }
        try {
            ProcessBuilder pb = new ProcessBuilder(pythonExec, bridgeScript, device.getDeviceUid(), "status");
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) out.append(line);
            p.waitFor();
            String resp = out.toString();
            log.info("MIoT status: did={}, resp={}", device.getDeviceUid(), resp);
            boolean ok = resp.contains("\"success\": true") || resp.contains("\"success\":true");
            result.put("success", ok);
            result.put("raw", resp);
            result.put("deviceName", device.getDeviceName());
            result.put("deviceUid", device.getDeviceUid());
            result.put("power", resp.contains("\"power\": true") || resp.contains("\"power\":true"));
            if (!ok) {
                result.put("msg", resp);
            }
        } catch (Exception e) {
            log.error("MIoT status failed: {}", e.getMessage());
            result.put("success", false);
            result.put("msg", e.getMessage());
        }
        return result;
    }

    private boolean sendCommand(SeatDeviceVo device, boolean turnOn) {
        try {
            String action = turnOn ? "on" : "off";
            ProcessBuilder pb = new ProcessBuilder(pythonExec, bridgeScript, device.getDeviceUid(), action);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder out = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) out.append(line);
            p.waitFor();
            String resp = out.toString();
            log.info("MIoT result: did={}, action={}, resp={}", device.getDeviceUid(), action, resp);
            return resp.contains("\"success\": true") || resp.contains("\"success\":true");
        } catch (Exception e) {
            log.error("MIoT failed: {}", e.getMessage());
            return false;
        }
    }

    private void updateDeviceStatus(Long id, Integer status) {
        SeatDeviceBo bo = new SeatDeviceBo();
        bo.setDeviceId(id);
        bo.setStatus(status);
        seatDeviceService.updateByBo(bo);
    }

    public List<SeatDeviceVo> getRoomDevices(Long roomId) {
        return seatDeviceService.queryByRoomId(roomId);
    }
}
