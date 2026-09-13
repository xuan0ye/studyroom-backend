package com.ruoyi.studyroom.wx.api;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.constant.PayConstants;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.studyroom.domain.DeviceControlRequest;
import com.ruoyi.studyroom.domain.bo.DayCardConsumptionBo;
import com.ruoyi.studyroom.domain.bo.OrderBo;
import com.ruoyi.studyroom.domain.bo.SeatLockBo;
import com.ruoyi.studyroom.domain.vo.DayCardConsumptionVo;
import com.ruoyi.studyroom.domain.vo.OrderVo;
import com.ruoyi.studyroom.domain.vo.SeatDeviceVo;
import com.ruoyi.studyroom.domain.vo.SeatLockVo;
import com.ruoyi.studyroom.service.IDayCardConsumptionService;
import com.ruoyi.studyroom.service.IOrderService;
import com.ruoyi.studyroom.service.ISeatDeviceService;
import com.ruoyi.studyroom.service.ISeatLockService;
import com.ruoyi.studyroom.service.MiotService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @ProjectName: study-room-vue
 * @Author: AI Generated
 * @Date: 2026-05-18
 * @Desc: 米家IoT设备控制接口(仅允许控制本人当日预约的座位设备)
 */
@Slf4j
@Api(value = "米家设备控制", tags = "米家设备控制")
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/wx/device")
public class DeviceController extends BaseController {

    private final MiotService miotService;
    private final ISeatDeviceService seatDeviceService;
    private final IDayCardConsumptionService dayCardConsumptionService;
    private final IOrderService orderService;
    private final ISeatLockService seatLockService;
    private final com.ruoyi.studyroom.service.ICardUserService cardUserService;

    /**
     * 校验当前用户是否对该座位拥有当日有效预约(日卡预约或进行中的按小时订单)
     */
    private boolean canControlSeat(Long userId, Long seatId) {
        if (userId == null || seatId == null) {
            return false;
        }
        Date now = new Date();
        Date today = DateUtil.beginOfDay(now);
        Date tomorrow = DateUtil.offsetDay(today, 1);
        // 1) 今日日卡预约(0待使用 或 1已使用: 已开灯签到后仍需能关灯/控灯)
        DayCardConsumptionBo bo = new DayCardConsumptionBo();
        bo.setUserId(userId);
        bo.setSeatId(seatId);
        List<DayCardConsumptionVo> cons = dayCardConsumptionService.queryList(bo);
        for (DayCardConsumptionVo c : cons) {
            if (c.getConsumptionDate() != null
                && !c.getConsumptionDate().before(today)
                && c.getConsumptionDate().before(tomorrow)
                && (c.getStatus() == null || c.getStatus() == 0 || c.getStatus() == 1)) {
                return true;
            }
        }
        // 2) 进行中的按小时订单(未消费且当前时间在时段内)
        OrderBo ob = new OrderBo();
        ob.setUserId(userId);
        ob.setSeatId(seatId);
        ob.setPayStatus(PayConstants.NOT_CONSUMED);
        List<OrderVo> orders = orderService.queryList(ob);
        for (OrderVo o : orders) {
            if (o.getStartTime() != null && o.getEndTime() != null
                && !now.before(o.getStartTime()) && !now.after(o.getEndTime())) {
                return true;
            }
        }
        // 3) 本人会员卡锁座窗口覆盖今天(周卡/月卡/季卡等跨日锁座) —— 与 /daily/today 同源判定
        // 日卡预约的长期锁座落在 sr_card_user.lock_seat_id/lock_start_time/lock_end_time(不是 sr_seat_lock 表)
        try {
            com.ruoyi.studyroom.domain.bo.CardUserBo cub = new com.ruoyi.studyroom.domain.bo.CardUserBo();
            cub.setUserId(userId);
            List<com.ruoyi.studyroom.domain.vo.CardUserVo> cards = cardUserService.queryList(cub);
            Date dayStart = DateUtil.beginOfDay(now);
            Date dayEnd = DateUtil.offsetDay(dayStart, 1);
            if (cards != null) {
                for (com.ruoyi.studyroom.domain.vo.CardUserVo cu : cards) {
                    if (cu.getLockSeatId() == null || !cu.getLockSeatId().equals(seatId)) {
                        continue;
                    }
                    if (cu.getLockStartTime() != null && cu.getLockEndTime() != null
                        && cu.getLockStartTime().before(dayEnd) && cu.getLockEndTime().after(dayStart)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("查询会员卡锁座失败 userId={}, seatId={}", userId, seatId, e);
        }
        // 4) sr_seat_lock 有效锁座(订阅/锁座通道, 兼容保留)
        SeatLockBo lb = new SeatLockBo();
        lb.setUserId(userId);
        lb.setSeatId(seatId);
        List<SeatLockVo> locks = seatLockService.queryList(lb);
        for (SeatLockVo l : locks) {
            // 状态: 0-待激活 1-激活(有效); 时间以日期粒度判断覆盖今日
            if (l.getStatus() != null && (l.getStatus() == 0 || l.getStatus() == 1)
                && l.getStartDate() != null && l.getEndDate() != null
                && !l.getEndDate().before(DateUtil.beginOfDay(now))) {
                return true;
            }
        }
        return false;
    }

    @ApiOperation("获取用户当前座位的设备列表")
    @GetMapping("/mine")
    public R<List<SeatDeviceVo>> myDevices(@RequestParam Long seatId) {
        Long userId = LoginHelper.getUserId();
        if (!canControlSeat(userId, seatId)) {
            return R.fail("您今日未预约该座位，无法查看设备");
        }
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
        Long userId = LoginHelper.getUserId();
        Map<String, Object> result;
        if (request.getSeatId() != null) {
            if (!canControlSeat(userId, request.getSeatId())) {
                return R.fail("您今日未预约该座位，无法控制设备");
            }
            result = request.getTurnOn() ?
                miotService.turnOnSeatDevices(request.getSeatId()) :
                miotService.turnOffSeatDevices(request.getSeatId());
        } else if (request.getDeviceId() != null) {
            // 按设备ID控制时, 校验设备归属的座位是否为本人当日预约
            SeatDeviceVo device = seatDeviceService.queryById(request.getDeviceId());
            if (device == null) {
                return R.fail("设备不存在");
            }
            if (!canControlSeat(userId, device.getSeatId())) {
                return R.fail("您今日未预约该座位，无法控制设备");
            }
            result = miotService.controlDevice(request.getDeviceId(), request.getTurnOn());
        } else {
            return R.fail("请指定 seatId 或 deviceId");
        }
        return R.ok(result);
    }

    @ApiOperation("打开座位电源(仅本人当日预约座位)")
    @PostMapping("/seat/on")
    public R<Map<String, Object>> seatPowerOn(@RequestParam Long seatId) {
        Long userId = LoginHelper.getUserId();
        if (!canControlSeat(userId, seatId)) {
            return R.fail("您今日未预约该座位，无法打开电源");
        }
        Map<String, Object> result = miotService.turnOnSeatDevices(seatId);
        // 开灯即签到: 记录当天首次开灯时间(连续签到/早到榜)
        try {
            boolean first = dayCardConsumptionService.checkInByLightOn(seatId, userId);
            if (first) {
                log.info("用户 {} 开灯签到成功, seatId={}", userId, seatId);
            }
        } catch (Exception e) {
            log.error("开灯签到失败: seatId={}", seatId, e);
        }
        return R.ok(result);
    }

    @ApiOperation("关闭座位电源(仅本人当日预约座位)")
    @PostMapping("/seat/off")
    public R<Map<String, Object>> seatPowerOff(@RequestParam Long seatId) {
        Long userId = LoginHelper.getUserId();
        if (!canControlSeat(userId, seatId)) {
            return R.fail("您今日未预约该座位，无法关闭电源");
        }
        Map<String, Object> result = miotService.turnOffSeatDevices(seatId);
        return R.ok(result);
    }
}
