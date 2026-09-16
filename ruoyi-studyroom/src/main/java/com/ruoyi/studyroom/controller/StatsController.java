package com.ruoyi.studyroom.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.studyroom.domain.*;
import com.ruoyi.studyroom.mapper.*;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 首页统计数据
 *
 * <p>提供后台首页真实统计: 用户数/座位数/订单数/卡用户数/今日数据/订单金额等</p>
 *
 * @author studyroom
 */
@Validated
@Api(value = "首页统计控制器", tags = {"首页统计"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/studyroom/stats")
public class StatsController extends BaseController {

    private final UserMapper userMapper;
    private final SeatMapper seatMapper;
    private final RoomMapper roomMapper;
    private final OrderMapper orderMapper;
    private final CardUserMapper cardUserMapper;
    private final CardMapper cardMapper;
    private final SeatRecordMapper seatRecordMapper;
    private final OrderCardMapper orderCardMapper;
    private final OrderRechargeMapper orderRechargeMapper;

    /**
     * 首页统计概览
     */
    @ApiOperation("首页统计概览")
    @SaCheckPermission("studyroom:stats:list")
    @GetMapping("/overview")
    public R<Map<String, Object>> overview() {
        Map<String, Object> data = new HashMap<>();

        // 用户数
        data.put("userCount", userMapper.selectCount(null));

        // 自习室数
        data.put("roomCount", roomMapper.selectCount(null));

        // 座位数
        data.put("seatCount", seatMapper.selectCount(null));

        // 订单总数
        data.put("orderCount", orderMapper.selectCount(null));

        // 用户持卡数(有效卡: 未激活的永久有效卡也算)
        LambdaQueryWrapper<CardUser> cardUserWrapper = new LambdaQueryWrapper<>();
        cardUserWrapper.and(w -> w.isNull(CardUser::getExpiryTime).or().ge(CardUser::getExpiryTime, new Date()));
        data.put("activeCardCount", cardUserMapper.selectCount(cardUserWrapper));

        // 卡种类数
        data.put("cardTypeCount", cardMapper.selectCount(null));

        // 今日订单数
        LambdaQueryWrapper<Order> todayOrderWrapper = new LambdaQueryWrapper<>();
        todayOrderWrapper.ge(Order::getStartTime, cn.hutool.core.date.DateUtil.beginOfDay(new Date()))
            .lt(Order::getStartTime, cn.hutool.core.date.DateUtil.offsetDay(cn.hutool.core.date.DateUtil.beginOfDay(new Date()), 1));
        data.put("todayOrderCount", orderMapper.selectCount(todayOrderWrapper));

        // 今日占座记录数
        LambdaQueryWrapper<SeatRecord> todayRecordWrapper = new LambdaQueryWrapper<>();
        todayRecordWrapper.ge(SeatRecord::getStartTime, cn.hutool.core.date.DateUtil.beginOfDay(new Date()))
            .lt(SeatRecord::getStartTime, cn.hutool.core.date.DateUtil.offsetDay(cn.hutool.core.date.DateUtil.beginOfDay(new Date()), 1));
        data.put("todayRecordCount", seatRecordMapper.selectCount(todayRecordWrapper));

        // 订单总金额(total 单位: 点, 1点=1元)
        List<Order> orders = orderMapper.selectList(null);
        long totalPoints = 0;
        for (Order o : orders) {
            if (o.getTotal() != null) {
                totalPoints += o.getTotal().longValue();
            }
        }
        data.put("totalAmountYuan", totalPoints);

        // 购卡总金额(元)
        List<OrderCard> orderCards = orderCardMapper.selectList(null);
        BigDecimal cardAmount = BigDecimal.ZERO;
        for (OrderCard oc : orderCards) {
            if (oc.getAmountTotal() != null) {
                cardAmount = cardAmount.add(oc.getAmountTotal());
            }
        }
        data.put("cardAmountYuan", cardAmount);

        // 充值总金额(元)
        List<OrderRecharge> recharges = orderRechargeMapper.selectList(null);
        BigDecimal rechargeAmount = BigDecimal.ZERO;
        for (OrderRecharge r : recharges) {
            if (r.getAmountTotal() != null) {
                rechargeAmount = rechargeAmount.add(r.getAmountTotal());
            }
        }
        data.put("rechargeAmountYuan", rechargeAmount);

        return R.ok(data);
    }

    /**
     * 座位预约状态统计(总数/已占用/空闲)
     */
    @ApiOperation("座位预约状态统计")
    @SaCheckPermission("studyroom:stats:list")
    @GetMapping("/seatStatus")
    public R<Map<String, Object>> seatStatus() {
        Map<String, Object> data = new HashMap<>();
        long total = seatMapper.selectCount(null);
        // 今日有占座记录的座位
        Date todayStart = cn.hutool.core.date.DateUtil.beginOfDay(new Date());
        Date todayEnd = cn.hutool.core.date.DateUtil.offsetDay(todayStart, 1);
        LambdaQueryWrapper<SeatRecord> wrapper = new LambdaQueryWrapper<>();
        wrapper.ge(SeatRecord::getStartTime, todayStart).lt(SeatRecord::getStartTime, todayEnd);
        long occupied = seatRecordMapper.selectCount(wrapper);
        data.put("total", total);
        data.put("occupied", occupied);
        data.put("free", Math.max(0, total - occupied));
        return R.ok(data);
    }
}
