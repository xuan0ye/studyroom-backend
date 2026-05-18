package com.ruoyi.studyroom.wx.api;

import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.studyroom.domain.DeviceControlRequest;
import com.ruoyi.studyroom.domain.vo.SeatDeviceVo;
import com.ruoyi.studyroom.service.ISeatDeviceService;
import com.ruoyi.studyroom.service.MiotService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * @ProjectName: study-room-vue
 * @Author: AI Generated
 * @Date: 2026-05-18
 * @Desc: 米家IoT设备控制接口
 */
@Api(value = "米家设备控制", tags = "米家设备控制")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/wx/device")
public class DeviceController extends BaseController {

    private final MiotService miotService;
    private final ISeatDeviceService seatDeviceService;

    @ApiOperation("获取用户当前座位的设备列表")
    @GetMapping("/mine")
    public R<List<SeatDeviceVo>> myDevices(@RequestParam Long seatId) {
        List<SeatDeviceVo> list = seatDeviceService.queryBySeatId(seatId);
        return R.ok(list);
    }

    @ApiOperation("获取房间所有设备")
    @GetMapping("/room")
    public R<List<SeatDeviceVo>> roomDevices(@RequestParam Long roomId) {
        List<SeatDeviceVo> list = miotService.getRoomDevices(roomId);
        return R.ok(list);
    }

    @ApiOperation("手动控制设备开关")
    @PostMapping("/control")
    public R<Map<String, Object>> controlDevice(@RequestBody DeviceControlRequest request) {
        Map<String, Object> result;
        if (request.getSeatId() != null) {
            result = request.getTurnOn() ?
                miotService.turnOnSeatDevices(request.getSeatId()) :
                miotService.turnOffSeatDevices(request.getSeatId());
        } else if (request.getDeviceId() != null) {
            result = miotService.controlDevice(request.getDeviceId(), request.getTurnOn());
        } else {
            return R.fail("请指定 seatId 或 deviceId");
        }
        return R.ok(result);
    }

    @ApiOperation("打开座位电源(预约成功自动调用)")
    @PostMapping("/seat/on")
    public R<Map<String, Object>> seatPowerOn(@RequestParam Long seatId) {
        Map<String, Object> result = miotService.turnOnSeatDevices(seatId);
        return R.ok(result);
    }

    @ApiOperation("关闭座位电源(预约结束自动调用)")
    @PostMapping("/seat/off")
    public R<Map<String, Object>> seatPowerOff(@RequestParam Long seatId) {
        Map<String, Object> result = miotService.turnOffSeatDevices(seatId);
        return R.ok(result);
    }
}
