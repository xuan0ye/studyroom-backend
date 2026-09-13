package com.ruoyi.studyroom.wx.api;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.studyroom.domain.bo.AccessDeviceLoginBo;
import com.ruoyi.studyroom.service.IAccessControlService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Api(value = "门禁设备桥接", tags = "门禁设备桥接")
@RestController
@RequiredArgsConstructor
public class AccessDeviceBridgeController {

    private final IAccessControlService accessControlService;

    @ApiOperation("门禁设备登录(兼容商家文档 faceDeivce/login)")
    @PostMapping("/api/faceDeivce/login")
    public R<Map<String, Object>> login(@RequestBody AccessDeviceLoginBo bo) {
        return R.ok(accessControlService.buildDeviceLoginResponse(bo));
    }

    @ApiOperation("接收门禁设备上报数据(由MQTT桥接调用)")
    @PostMapping("/api/faceDevice/report")
    public R<Boolean> report(@RequestBody Map<String, Object> payload) {
        accessControlService.handleMqttDataReport(payload);
        return R.ok(true);
    }
}
