package com.ruoyi.studyroom.wx.api;

import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.studyroom.domain.bo.SeatLockBo;
import com.ruoyi.studyroom.domain.vo.SeatLockVo;
import com.ruoyi.studyroom.service.ISeatLockService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

/**
 * @ProjectName: study-room-vue
 * @Author: WenZhengcheng
 * @Date: 2022-5-10
 * @Desc: 微信小程序 - 座位订阅/锁定控制器
 * 安全加固: lock/unlock/renew 全部做本人归属校验与字段白名单, 锁定结束日期由服务端按类型计算并设上限
 */

@Api(value = "座位订阅/锁定控制器", tags = {"座位订阅/锁定"})
@Validated
@RequiredArgsConstructor
@RestController
@RequestMapping("/api/wx/subscription")
public class SubscriptionController {

    private final ISeatLockService seatLockService;

    /** 单次锁定/续期天数上限(统一收敛, 防止 3650 天等超长占座) */
    private static final int MAX_LOCK_DAYS = 365;

    @ApiOperation("锁定座位")
    @PostMapping("/lock")
    public R<Void> lock(@RequestBody SeatLockBo bo) {
        if (bo.getSeatId() == null) {
            return R.fail("请选择座位");
        }
        Date now = new Date();
        if (bo.getStartDate() == null) {
            bo.setStartDate(now);
        }
        // 结束日期服务端按类型计算并校验(不允许客户端任意延长)
        Date end = bo.getEndDate() == null
            ? DateUtil.offsetDay(now, lockDaysByType(bo.getLockType()))
            : bo.getEndDate();
        if (!end.after(now)) {
            return R.fail("锁定结束时间必须晚于当前时间");
        }
        if (end.after(DateUtil.offsetDay(now, MAX_LOCK_DAYS + 1))) {
            return R.fail("单次锁定最长 " + MAX_LOCK_DAYS + " 天");
        }
        bo.setEndDate(end);
        bo.setUserId(LoginHelper.getUserId());
        bo.setStatus(0); // 0-待激活
        return toAjax(seatLockService.insertByBo(bo));
    }

    @ApiOperation("解锁座位")
    @PostMapping("/unlock")
    public R<Void> unlock(@RequestBody SeatLockBo bo) {
        if (bo.getId() == null) {
            return R.fail("参数错误");
        }
        SeatLockVo vo = seatLockService.queryById(bo.getId());
        if (vo == null) {
            return R.fail("记录不存在");
        }
        if (vo.getUserId() == null || !vo.getUserId().equals(LoginHelper.getUserId())) {
            return R.fail("无权操作该锁座记录");
        }
        // 字段白名单: 仅允许变更状态为已取消
        SeatLockBo upd = new SeatLockBo();
        upd.setId(vo.getId());
        upd.setStatus(3); // 3-已取消
        return toAjax(seatLockService.updateByBo(upd));
    }

    @ApiOperation("我的订阅列表")
    @GetMapping("/mine")
    public R<List<SeatLockVo>> mine() {
        SeatLockBo bo = new SeatLockBo();
        bo.setUserId(LoginHelper.getUserId());
        return R.ok(seatLockService.queryList(bo));
    }

    @ApiOperation("续期锁定")
    @PostMapping("/renew")
    public R<Void> renew(@RequestBody SeatLockBo bo) {
        if (bo.getId() == null) {
            return R.fail("参数错误");
        }
        SeatLockVo vo = seatLockService.queryById(bo.getId());
        if (vo == null) {
            return R.fail("记录不存在");
        }
        if (vo.getUserId() == null || !vo.getUserId().equals(LoginHelper.getUserId())) {
            return R.fail("无权操作该锁座记录");
        }
        // 续期天数按原 lockType 服务端计算, 不接受客户端 endDate
        int days = Math.min(lockDaysByType(vo.getLockType()), MAX_LOCK_DAYS);
        Date newEnd = DateUtil.offsetDay(new Date(), days);
        SeatLockBo upd = new SeatLockBo();
        upd.setId(vo.getId());
        upd.setEndDate(newEnd);
        upd.setAutoRenew(1);
        return toAjax(seatLockService.updateByBo(upd));
    }

    /** 锁座类型 -> 天数: 1-日卡1天, 2-周/季类90天, 3-年卡365天, 其它默认30天(原3650天档已收敛) */
    private int lockDaysByType(Integer lockType) {
        if (lockType == null) {
            return 30;
        }
        if (lockType == 1) {
            return 1;
        }
        if (lockType == 2) {
            return 90;
        }
        if (lockType == 3) {
            return 365;
        }
        return 30;
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
