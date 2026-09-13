package com.ruoyi.studyroom.wx.probe;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

/**
 * 门禁设备协议探测控制器
 * <p>
 * 设备把服务器地址指向本系统后, 会按私有协议请求任意路径。
 * 本控制器 catch-all 接收这些请求, 返回宽松 JSON, 让设备认为连接成功、
 * 继续发送后续报文(心跳/上报/人脸等), 配合 DeviceProbeFilter 持续抓包。
 */
@Slf4j
@Api(value = "门禁设备协议探测", tags = "门禁设备协议探测")
@RestController
public class DeviceProbeController {

    @Value("${studyroom.access.probe.response:{\"code\":0,\"msg\":\"success\"}}")
    private String probeResponse;

    @ApiOperation("设备协议探测 catch-all")
    @RequestMapping(value = {"/server", "/server/**", "/access", "/access/**", "/api/faceDeivce/**", "/api/faceDevice/**"}, method = {RequestMethod.GET, RequestMethod.POST, RequestMethod.PUT, RequestMethod.DELETE, RequestMethod.OPTIONS})
    public Map<String, Object> probe(HttpServletRequest request) {
        log.info("[PROBE-CONTROLLER] {} {}", request.getMethod(), request.getRequestURI());
        Map<String, Object> map = new HashMap<>();
        // 宽松响应: 让设备继续发包
        map.put("code", 0);
        map.put("msg", "success");
        return map;
    }
}
