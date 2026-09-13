package com.ruoyi.studyroom.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.studyroom.domain.bo.SeatDeviceBo;
import com.ruoyi.studyroom.domain.vo.SeatDeviceVo;
import com.ruoyi.studyroom.service.ISeatDeviceService;
import com.ruoyi.studyroom.service.MiotService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 灯控自检(米家桥接健康状态)
 *
 * 说明: 座位灯通过服务器上的 Python 桥接脚本 /deploy/miot_bridge.py 调用米家接口,
 * 依赖米家登录态文件 /root/.config/mijia-api/auth.json。该文件失效时所有灯控都会失败,
 * 本控制器用于在后台查看健康状态、手动自检、以及重新生成登录二维码(扫码即可修复)。
 *
 * @author ruoyi
 */
@Slf4j
@Validated
@Api(value = "灯控自检", tags = {"灯控自检"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/studyroom/miot")
public class MiotHealthController extends BaseController {

    /** 定时巡检日志(由 /deploy/miot_health.sh 写入) */
    private static final String HEALTH_LOG = "/root/app/miot_health.log";
    /** 米家登录态文件 */
    private static final String AUTH_FILE = "/root/.config/mijia-api/auth.json";
    /** 重新登录脚本(生成二维码并等待扫码) */
    private static final String RELOGIN_SCRIPT = "/deploy/mijia_relogin.sh";
    /** 登录脚本输出(含二维码链接) */
    private static final String RELOGIN_LOG = "/tmp/mijia_login.log";
    /** 二维码图片(脚本生成) */
    private static final String QR_SRC = "/deploy/mijia_qr.png";
    /** 上传目录(nginx /upload/ 由后端转发, 可直接被后台页面引用) */
    private static final String UPLOAD_DIR = "/root/app/upload/appstatic/";
    private static final String QR_WEB_PATH = "/upload/appstatic/mijia_qr.png";

    private final MiotService miotService;
    private final ISeatDeviceService seatDeviceService;

    @Value("${studyroom.miot.bridge-script:/deploy/miot_bridge.py}")
    private String bridgeScript;

    @Value("${studyroom.miot.python:/usr/bin/python3}")
    private String pythonExec;

    /**
     * 灯控健康状态(读巡检日志与认证文件, 不访问设备)
     */
    @ApiOperation("灯控健康状态")
    @SaCheckPermission("studyroom:miot:health")
    @GetMapping("/health")
    public R<Map<String, Object>> health() {
        return R.ok(buildHealth());
    }

    /**
     * 立即自检: 取一台绑定设备做只读状态查询(不改变开关状态)
     */
    @ApiOperation("立即自检(只读查询一台设备)")
    @SaCheckPermission("studyroom:miot:health")
    @PostMapping("/selfcheck")
    public R<Map<String, Object>> selfCheck() {
        Map<String, Object> result = new HashMap<>();
        Long deviceId = pickProbeDeviceId();
        if (deviceId == null) {
            result.put("success", false);
            result.put("msg", "没有可用的座位设备(请先在门店管理-座位里绑定设备)");
            return R.ok(result);
        }
        Map<String, Object> probe = miotService.queryDeviceStatus(deviceId);
        boolean ok = Boolean.TRUE.equals(probe.get("success"));
        String ts = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        String line = ts + " [" + (ok ? "OK" : "FAIL") + "] 自检: " + probe.get("deviceName")
            + " " + (ok ? ("当前(" + (Boolean.TRUE.equals(probe.get("power")) ? "开" : "关") + ") " + probe.get("raw"))
                        : probe.get("msg"));
        appendLog(line);
        result.put("success", ok);
        result.put("probe", probe);
        result.put("msg", ok ? "灯控正常" : ("灯控异常: " + probe.get("msg")));
        result.put("health", buildHealth());
        return R.ok(result);
    }

    /**
     * 生成米家登录二维码(扫码后自动写好登录态, 无需手工执行命令)
     */
    @ApiOperation("生成米家登录二维码")
    @SaCheckPermission("studyroom:miot:health")
    @PostMapping("/relogin")
    public R<Map<String, Object>> relogin() {
        Map<String, Object> result = new HashMap<>();
        String out = runCommand(45, "bash", RELOGIN_SCRIPT);
        result.put("output", out);
        // 登录态仍有效时脚本直接返回 ALREADY_OK, 不会生成二维码
        if (out.contains("ALREADY_OK")) {
            result.put("success", true);
            result.put("alreadyOk", true);
            result.put("msg", "当前米家登录态有效，无需重新登录");
            return R.ok(result);
        }
        File qr = new File(QR_SRC);
        if (!qr.exists()) {
            result.put("success", false);
            result.put("msg", "二维码生成失败, 请查看脚本输出");
            return R.ok(result);
        }
        try {
            Path dest = Paths.get(UPLOAD_DIR + "mijia_qr.png");
            Files.createDirectories(dest.getParent());
            Files.copy(qr.toPath(), dest, StandardCopyOption.REPLACE_EXISTING);
            result.put("success", true);
            result.put("qrUrl", QR_WEB_PATH);
            result.put("qrStamp", System.currentTimeMillis());
            result.put("qrTime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(qr.lastModified())));
            // 也返回米家原始二维码链接, 便于前端直接展示
            String qrLink = readFirstMatch(RELOGIN_LOG, "QR_URL=(\\S+)");
            result.put("qrLink", qrLink);
            result.put("msg", "请用手机米家App扫码完成登录(二维码几分钟内有效)");
        } catch (Exception e) {
            log.error("复制登录二维码失败", e);
            result.put("success", false);
            result.put("msg", "二维码已生成但复制失败: " + e.getMessage());
        }
        return R.ok(result);
    }

    // ==================== 内部工具 ====================

    private Map<String, Object> buildHealth() {
        Map<String, Object> map = new HashMap<>();
        // 1. 认证文件
        File auth = new File(AUTH_FILE);
        map.put("authFile", AUTH_FILE);
        map.put("authExists", auth.exists());
        if (auth.exists()) {
            String content = readAll(AUTH_FILE);
            map.put("authExpire", formatEpoch(readFirstMatchValue(content, "\"expireTime\"\\s*:\\s*(\\d+)")));
            map.put("authSaveTime", formatEpoch(readFirstMatchValue(content, "\"saveTime\"\\s*:\\s*(\\d+)")));
            map.put("authSize", auth.length());
        }
        // 2. 桥接脚本与 python
        map.put("bridgeScript", bridgeScript);
        map.put("bridgeExists", new File(bridgeScript).exists());
        map.put("python", pythonExec);
        map.put("pythonExists", new File(pythonExec).exists());
        // 3. 巡检日志
        List<String> tail = readTail(HEALTH_LOG, 12);
        map.put("logTail", tail);
        String last = null;
        String lastFail = null;
        for (String line : tail) {
            if (line.contains("[OK]") || line.contains("[FAIL]") || line.contains("[ERROR]")) {
                last = line;
            }
            if (line.contains("[FAIL]") || line.contains("[ACTION]") || line.contains("[ERROR]")) {
                lastFail = line;
            }
        }
        map.put("lastCheck", last);
        map.put("lastFail", lastFail);
        map.put("healthy", last != null && last.contains("[OK]") && !authMissing(auth));
        return map;
    }

    private boolean authMissing(File auth) {
        return !auth.exists();
    }

    /** 取一台绑定设备做只读探测(优先 22 号座位那台, 保证与巡检脚本一致) */
    private Long pickProbeDeviceId() {
        try {
            SeatDeviceBo bo = new SeatDeviceBo();
            List<SeatDeviceVo> list = seatDeviceService.queryList(bo);
            if (list == null || list.isEmpty()) {
                return null;
            }
            for (SeatDeviceVo d : list) {
                if ("1163435435".equals(d.getDeviceUid())) {
                    return d.getDeviceId();
                }
            }
            return list.get(0).getDeviceId();
        } catch (Exception e) {
            log.error("取探测设备失败", e);
            return null;
        }
    }

    /** 追加一行到巡检日志(保留最近 500 行) */
    private void appendLog(String line) {
        try {
            Path p = Paths.get(HEALTH_LOG);
            List<String> lines = new ArrayList<>();
            if (Files.exists(p)) {
                lines.addAll(Files.readAllLines(p, StandardCharsets.UTF_8));
            }
            lines.add(line);
            if (lines.size() > 500) {
                lines = lines.subList(lines.size() - 500, lines.size());
            }
            Files.write(p, lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("写巡检日志失败", e);
        }
    }

    private List<String> readTail(String path, int n) {
        try {
            Path p = Paths.get(path);
            if (!Files.exists(p)) {
                return new ArrayList<>();
            }
            List<String> all = Files.readAllLines(p, StandardCharsets.UTF_8);
            if (all.size() <= n) {
                return all;
            }
            return new ArrayList<>(all.subList(all.size() - n, all.size()));
        } catch (Exception e) {
            log.error("读巡检日志失败", e);
            return new ArrayList<>();
        }
    }

    private String readAll(String path) {
        try {
            return new String(Files.readAllBytes(Paths.get(path)), StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }

    private String readFirstMatch(String path, String regex) {
        return readFirstMatchValue(readAll(path), regex);
    }

    private String readFirstMatchValue(String content, String regex) {
        if (content == null || content.isEmpty()) {
            return null;
        }
        Matcher m = Pattern.compile(regex).matcher(content);
        return m.find() ? m.group(1) : null;
    }

    /** 毫秒时间戳 -> 可读时间 */
    private String formatEpoch(String millis) {
        if (millis == null || millis.isEmpty()) {
            return null;
        }
        try {
            long ms = Long.parseLong(millis.trim());
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(ms));
        } catch (Exception e) {
            return millis;
        }
    }

    /** 执行外部命令并返回输出(带超时, 避免请求挂死) */
    private String runCommand(int timeoutSeconds, String... cmd) {
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            StringBuilder sb = new StringBuilder();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = r.readLine()) != null) {
                    sb.append(line).append('\n');
                }
            }
            if (!p.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
                p.destroyForcibly();
                sb.append("\n[超时 ").append(timeoutSeconds).append("s, 已强制结束]");
            }
            return sb.toString().trim();
        } catch (Exception e) {
            log.error("执行命令失败: {}", String.join(" ", cmd), e);
            return "执行失败: " + e.getMessage();
        }
    }
}
