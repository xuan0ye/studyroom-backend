package com.ruoyi.studyroom.wx.api;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.studyroom.domain.bo.DayCardConsumptionBo;
import com.ruoyi.studyroom.domain.bo.OrderBo;
import com.ruoyi.studyroom.domain.vo.DayCardConsumptionVo;
import com.ruoyi.studyroom.domain.vo.OrderVo;
import com.ruoyi.studyroom.domain.vo.RoomConfigVo;
import com.ruoyi.studyroom.service.IDayCardConsumptionService;
import com.ruoyi.studyroom.service.IOrderService;
import com.ruoyi.studyroom.service.IRoomConfigService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * @ProjectName: study-room-vue
 * @Author: WenZhengcheng
 * @Date: 2022-5-10
 * @Desc: 微信小程序 - 日卡/每日相关控制器
 */

@Api(value = "日卡/每日控制器", tags = {"日卡/每日"})
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/wx/daily")
public class DailyController {

    private final IDayCardConsumptionService dayCardConsumptionService;
    private final IOrderService orderService;
    private final IRoomConfigService roomConfigService;
    private final com.ruoyi.studyroom.service.ICardUserService cardUserService;
    private final com.ruoyi.studyroom.service.ISeatService seatService;

    /**
     * 我今天的座位: 覆盖周卡/月卡锁座期(锁座期间每天可用)与今日预约记录
     * 返回: [{seatId, seatNum, areaName, roomId, source}] source=lock(锁座)或book(今日预约)
     */
    @ApiOperation("我今天的座位(锁座/预约, 用于主页灯控)")
    @GetMapping("/today")
    public R<List<java.util.Map<String, Object>>> today() {
        Long userId = LoginHelper.getUserId();
        Date now = new Date();
        Date dayStart = cn.hutool.core.date.DateUtil.beginOfDay(now);
        Date dayEnd = cn.hutool.core.date.DateUtil.offsetDay(dayStart, 1);
        java.util.Map<Long, java.util.Map<String, Object>> result = new java.util.LinkedHashMap<>();
        try {
            // 1. 有效卡锁座(lock_start<=今天<lock_end 或 lock_start今天之前且lock_end未来)
            com.ruoyi.studyroom.domain.bo.CardUserBo cardBo = new com.ruoyi.studyroom.domain.bo.CardUserBo();
            cardBo.setUserId(userId);
            List<com.ruoyi.studyroom.domain.vo.CardUserVo> cards = cardUserService.queryList(cardBo);
            if (cards != null) {
                for (com.ruoyi.studyroom.domain.vo.CardUserVo cu : cards) {
                    if (cu.getLockSeatId() == null || cu.getLockStartTime() == null || cu.getLockEndTime() == null) continue;
                    // 锁座期覆盖今天: start < dayEnd 且 end > dayStart
                    if (cu.getLockStartTime().before(dayEnd) && cu.getLockEndTime().after(dayStart)) {
                        com.ruoyi.studyroom.domain.vo.SeatVo seat = seatService.queryById(cu.getLockSeatId());
                        if (seat != null) {
                            java.util.Map<String, Object> m = new java.util.HashMap<>();
                            m.put("seatId", seat.getSeatId());
                            m.put("seatNum", seat.getSeatNum());
                            m.put("roomId", seat.getRoomId());
                            m.put("source", "lock");
                            result.put(seat.getSeatId(), m);
                        }
                    }
                }
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(DailyController.class).error("查询锁座失败", e);
        }
        // 2. 今日消费预约记录(status=0 待使用/1已使用 且日期=今天)
        try {
            DayCardConsumptionBo bo = new DayCardConsumptionBo();
            bo.setUserId(userId);
            bo.setConsumptionDate(dayStart);
            List<DayCardConsumptionVo> list = dayCardConsumptionService.queryList(bo);
            if (list != null) {
                for (DayCardConsumptionVo d : list) {
                    if (d.getSeatId() == null) continue;
                    if (d.getConsumptionDate() != null
                        && !d.getConsumptionDate().before(dayStart)
                        && d.getConsumptionDate().before(dayEnd)
                        && (d.getStatus() == null || d.getStatus() == 0 || d.getStatus() == 1)) {
                        com.ruoyi.studyroom.domain.vo.SeatVo seat = seatService.queryById(d.getSeatId());
                        if (seat != null) {
                            java.util.Map<String, Object> m = new java.util.HashMap<>();
                            m.put("seatId", seat.getSeatId());
                            m.put("seatNum", seat.getSeatNum());
                            m.put("roomId", seat.getRoomId());
                            m.put("source", "book");
                            // 已签到(当天首次开灯)时间, 供主页灯控卡片展示
                            if (d.getCheckInTime() != null) {
                                m.put("checkInTime", d.getCheckInTime());
                            }
                            result.put(seat.getSeatId(), m);
                        }
                    }
                }
            }
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(DailyController.class).error("查询今日预约失败", e);
        }
        return R.ok(new java.util.ArrayList<>(result.values()));
    }

    @ApiOperation("日卡下单")
    @PostMapping("/order")
    public R<Void> order(@RequestBody DayCardConsumptionBo bo) {
        bo.setUserId(LoginHelper.getUserId());
        try {
            boolean flag = dayCardConsumptionService.bookByDayCard(bo);
            return toAjax(flag);
        } catch (com.ruoyi.common.exception.ServiceException e) {
            return R.fail(e.getMessage());
        }
    }

    @ApiOperation("今日日卡状态")
    @GetMapping("/status")
    public R<List<DayCardConsumptionVo>> status() {
        DayCardConsumptionBo bo = new DayCardConsumptionBo();
        bo.setUserId(LoginHelper.getUserId());
        return R.ok(dayCardConsumptionService.queryList(bo));
    }

    @ApiOperation("我的预约记录")
    @GetMapping("/record")
    public R<List<DayCardConsumptionVo>> record() {
        return R.ok(dayCardConsumptionService.queryMyRecords(LoginHelper.getUserId()));
    }

    @ApiOperation("取消预约(日卡/周卡不退款, 长期卡按比例退余额)")
    @PostMapping("/cancel")
    public R<String> cancel(@RequestBody com.ruoyi.studyroom.domain.bo.DayCardConsumptionBo bo) {
        try {
            String msg = dayCardConsumptionService.cancelByUser(bo.getId(), LoginHelper.getUserId());
            return R.ok(msg);
        } catch (com.ruoyi.common.exception.ServiceException e) {
            return R.fail(e.getMessage());
        }
    }

    @ApiOperation("月卡请假停卡(释放座位+有效期顺延, 每张月卡限2次)")
    @PostMapping("/leave")
    public R<String> leave(@RequestBody DayCardConsumptionBo bo) {
        try {
            String msg = dayCardConsumptionService.leaveByCard(bo.getCardUserId(), LoginHelper.getUserId());
            return R.ok(msg);
        } catch (com.ruoyi.common.exception.ServiceException e) {
            return R.fail(e.getMessage());
        }
    }

    @ApiOperation("周卡/月卡取消锁座(释放座位, 可重新预约, 不退款)")
    @PostMapping("/unlock")
    public R<String> unlock(@RequestBody DayCardConsumptionBo bo) {
        try {
            String msg = dayCardConsumptionService.unlockByCard(bo.getCardUserId(), LoginHelper.getUserId());
            return R.ok(msg);
        } catch (com.ruoyi.common.exception.ServiceException e) {
            return R.fail(e.getMessage());
        }
    }
    @ApiOperation("自习室配置")
    @GetMapping("/config")
    public R<List<RoomConfigVo>> config(@ApiParam("自习室ID") @NotNull @RequestParam Long roomId) {
        com.ruoyi.studyroom.domain.bo.RoomConfigBo bo = new com.ruoyi.studyroom.domain.bo.RoomConfigBo();
        bo.setRoomId(roomId);
        return R.ok(roomConfigService.queryList(bo));
    }

    /**
     * 转换为Ajax结果
     */
    private R<Void> toAjax(Boolean result) {
        if (Boolean.TRUE.equals(result)) {
            return R.ok();
        }
        return R.fail();
    }
}
