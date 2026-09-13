package com.ruoyi.studyroom.service;

import com.ruoyi.studyroom.domain.bo.DayCardConsumptionBo;
import com.ruoyi.studyroom.domain.bo.SeatDeviceBo;
import com.ruoyi.studyroom.domain.vo.DayCardConsumptionVo;
import com.ruoyi.studyroom.domain.vo.SeatDeviceVo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DailyResetTask {

    private final ISeatDeviceService seatDeviceService;
    private final IDayCardConsumptionService dayCardConsumptionService;
    private final ISeatLockService seatLockService;
    private final IOrderService orderService;
    private final MiotService miotService;

    @Scheduled(cron = "0 0 0 * * ?")
    public void dailyReset() {
        log.info("===== 每日凌晨重置任务开始 =====");
        turnOffAllDevices();
        expireOldDayCardConsumptions();
        expireOldDailyOrders();
        activateTodayLocks();
        expireOverdueLocks();
        log.info("===== 每日凌晨重置任务完成 =====");
    }

    private void expireOverdueLocks() {
        try {
            seatLockService.expireOverdueLocks();
            log.info("已清理超过有效期的座位锁定");
        } catch (Exception e) {
            log.error("清理过期座位锁定失败", e);
        }
    }

    private void turnOffAllDevices() {
        try {
            // 24小时营业: 当天有预约的座位保留设备状态(由用户控制), 只关闭无预约座位的设备
            java.util.Set<Long> bookedSeatIds = new java.util.HashSet<>();
            try {
                DayCardConsumptionBo dbo = new DayCardConsumptionBo();
                dbo.setStatus(0);
                List<DayCardConsumptionVo> list = dayCardConsumptionService.queryList(dbo);
                LocalDate today = LocalDate.now();
                Date todayStart = Date.from(today.atStartOfDay(ZoneId.systemDefault()).toInstant());
                Date tomorrowStart = Date.from(today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
                for (DayCardConsumptionVo vo : list) {
                    if (vo.getSeatId() != null && vo.getConsumptionDate() != null
                        && !vo.getConsumptionDate().before(todayStart)
                        && vo.getConsumptionDate().before(tomorrowStart)) {
                        bookedSeatIds.add(vo.getSeatId());
                    }
                }
                log.info("今日有预约的座位数: {}", bookedSeatIds.size());
            } catch (Exception e) {
                log.error("查询今日预约座位失败, 将关闭全部设备", e);
            }
            SeatDeviceBo bo = new SeatDeviceBo();
            bo.setStatus(2);
            List<SeatDeviceVo> devices = seatDeviceService.queryList(bo);
            int closed = 0;
            for (SeatDeviceVo device : devices) {
                if (device.getSeatId() != null && bookedSeatIds.contains(device.getSeatId())) {
                    continue; // 今日有预约, 保留灯亮
                }
                try {
                    miotService.controlDevice(device.getDeviceId(), false);
                    log.info("已关闭设备: {} (座位ID={})", device.getDeviceName(), device.getSeatId());
                    closed++;
                } catch (Exception e) {
                    log.error("关闭设备失败: {} (ID={})", device.getDeviceName(), device.getDeviceId(), e);
                }
            }
            log.info("已处理 {} 个座位设备, 其中关闭 {} 个(有预约的保留)", devices.size(), closed);
        } catch (Exception e) {
            log.error("关闭座位设备失败", e);
        }
    }

    private void expireOldDayCardConsumptions() {
        try {
            DayCardConsumptionBo bo = new DayCardConsumptionBo();
            bo.setStatus(0);
            List<DayCardConsumptionVo> list = dayCardConsumptionService.queryList(bo);
            LocalDate yesterday = LocalDate.now().minusDays(1);
            Date yesterdayDate = Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant());
            for (DayCardConsumptionVo vo : list) {
                if (vo.getConsumptionDate() != null && vo.getConsumptionDate().before(yesterdayDate)) {
                    DayCardConsumptionBo updateBo = new DayCardConsumptionBo();
                    updateBo.setId(vo.getId());
                    updateBo.setStatus(2);
                    dayCardConsumptionService.updateByBo(updateBo);
                }
            }
            log.info("已处理过期日卡消费记录");
        } catch (Exception e) {
            log.error("处理过期日卡消费记录失败", e);
        }
    }

    private void expireOldDailyOrders() {
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            orderService.expireDailyOrders(yesterday);
            log.info("已过期 {} 的订单", yesterday);
        } catch (Exception e) {
            log.error("过期订单失败", e);
        }
    }

    private void activateTodayLocks() {
        try {
            seatLockService.activateTodayLocks();
            log.info("已激活当天生效的座位锁定");
        } catch (Exception e) {
            log.error("激活座位锁定失败", e);
        }
    }
}