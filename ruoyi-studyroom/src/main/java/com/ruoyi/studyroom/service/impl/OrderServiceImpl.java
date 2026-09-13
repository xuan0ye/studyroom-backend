package com.ruoyi.studyroom.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.PayConstants;
import com.ruoyi.common.exception.ServiceException;

import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.studyroom.domain.bo.BalanceBo;
import com.ruoyi.studyroom.domain.bo.RecordBo;
import com.ruoyi.studyroom.domain.bo.SeatRecordBo;
import com.ruoyi.studyroom.domain.vo.BalanceVo;
import com.ruoyi.studyroom.domain.vo.RecordVo;
import com.ruoyi.studyroom.service.IBalanceService;
import com.ruoyi.studyroom.service.IRecordService;
import com.ruoyi.studyroom.service.ISeatRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ruoyi.studyroom.domain.bo.OrderBo;
import com.ruoyi.studyroom.domain.vo.OrderVo;
import com.ruoyi.studyroom.domain.Order;
import com.ruoyi.studyroom.mapper.OrderMapper;
import com.ruoyi.studyroom.service.IOrderService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.Objects;

/**
 * 订单Service业务层处理
 *
 * @author P_Peaceful
 * @date 2022-05-08
 */
@RequiredArgsConstructor
@Service
public class OrderServiceImpl implements IOrderService {

    private final OrderMapper baseMapper;
    private final ISeatRecordService seatRecordService;
    private final IBalanceService balanceService;
    private final IRecordService recordService;
    /**
     * 查询订单
     *
     * @param id 订单主键
     * @return 订单
     */
    @Override
    public OrderVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询订单列表
     *
     * @param bo 订单
     * @return 订单
     */
    @Override
    public TableDataInfo<OrderVo> queryPageList(OrderBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<Order> lqw = buildQueryWrapper(bo);
        Page<OrderVo> result = baseMapper.selectVoAndSeatPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询订单列表
     *
     * @param bo 订单
     * @return 订单
     */
    @Override
    public List<OrderVo> queryList(OrderBo bo) {
        LambdaQueryWrapper<Order> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<Order> buildQueryWrapper(OrderBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<Order> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getOrderId() != null, Order::getOrderId, bo.getOrderId());
        lqw.eq(bo.getUserId() != null, Order::getUserId, bo.getUserId());
        lqw.eq(bo.getRoomId() != null, Order::getRoomId, bo.getRoomId());
        lqw.eq(bo.getSeatId() != null, Order::getSeatId, bo.getSeatId());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), Order::getDescription, bo.getDescription());
        lqw.eq(bo.getStartTime() != null, Order::getStartTime, bo.getStartTime());
        lqw.eq(bo.getEndTime() != null, Order::getEndTime, bo.getEndTime());
        lqw.eq(bo.getPayStatus() != null, Order::getPayStatus, bo.getPayStatus());
        return lqw;
    }

    /**
     * 新增订单
     *
     * @param bo 订单
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean insertByBo(OrderBo bo) {
        // 同座位时段冲突校验: 已支付未消费的订单仍占用座位, 防止重复预约
        if (bo.getSeatId() != null && bo.getStartTime() != null && bo.getEndTime() != null) {
            LambdaQueryWrapper<Order> conflict = Wrappers.lambdaQuery();
            conflict.eq(Order::getSeatId, bo.getSeatId())
                .eq(Order::getPayStatus, PayConstants.NOT_CONSUMED)
                .lt(Order::getStartTime, bo.getEndTime())
                .gt(Order::getEndTime, bo.getStartTime());
            if (baseMapper.selectCount(conflict) > 0) {
                throw new ServiceException("该座位在该时段已被预约，请重新选座");
            }
        }
        //使用账户余额预定座位: 原子扣减(防止并发超扣), 余额不足抛异常由事务回滚
        if (Constants.PAY_BALANCE.equals(bo.getPayMethod())){
            if (bo.getTotal() == null || bo.getTotal() <= 0) {
                throw new ServiceException("订单金额异常");
            }
            balanceService.deduct(java.math.BigDecimal.valueOf(bo.getTotal()), LoginHelper.getUserId());
        } else {
            //todo 使用消费卡预定座位 逻辑
        }
        Order add = BeanUtil.toBean(bo, Order.class);
        SeatRecordBo seatRecordBo = BeanUtil.toBean(bo, SeatRecordBo.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        //同步更新座位-记录表
        seatRecordService.insertByBo(seatRecordBo);
        if (flag) {
            bo.setId(add.getId());
        }

        return flag;
    }

    /**
     * 修改订单
     *
     * @param bo 订单
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateByBo(OrderBo bo) {
        Order update = BeanUtil.toBean(bo, Order.class);
        if (PayConstants.CONSUMED.equals(bo.getPayStatus())){
            RecordVo recordVo = recordService.queryByUserId(bo.getUserId());
            RecordBo recordBo = BeanUtil.toBean(recordVo, RecordBo.class);
            recordBo.setHours(recordVo.getHours()+ bo.getHours());
            recordService.updateByBo(recordBo);
        }
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 更新订单状态为已消费，同步更新学习记录排行榜
     *
     * @param orderBo
     * @return
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateByBoAndRank(OrderBo orderBo) throws Exception {
        if (orderBo.getOrderId() == null) {
            throw new ServiceException("参数错误");
        }
        Long userId = LoginHelper.getUserId();
        // 归属+状态闸门: 只能消费本人"未消费"的订单, 防重复累加学习时长/篡改他人订单
        LambdaQueryWrapper<Order> q = Wrappers.lambdaQuery();
        q.eq(Order::getOrderId, orderBo.getOrderId())
            .eq(Order::getUserId, userId)
            .eq(Order::getPayStatus, PayConstants.NOT_CONSUMED)
            .last("limit 1");
        Order order = baseMapper.selectOne(q);
        if (order == null) {
            Long cnt = baseMapper.selectCount(Wrappers.<Order>lambdaQuery().eq(Order::getOrderId, orderBo.getOrderId()));
            throw new ServiceException(cnt != null && cnt > 0 ? "该订单不可重复消费" : "订单不存在");
        }
        // 学习时长以订单起止时段由服务端计算(客户端 hours 不可信, 防刷榜)
        double hours = 0;
        if (order.getStartTime() != null && order.getEndTime() != null) {
            long ms = order.getEndTime().getTime() - order.getStartTime().getTime();
            hours = ms > 0 ? Math.round(ms / 3600000.0 * 10) / 10.0 : 0;
        }
        // 状态机推进 NOT_CONSUMED(3) -> CONSUMED(4), 带条件更新保证幂等
        boolean ok = baseMapper.update(null, Wrappers.<Order>lambdaUpdate()
            .eq(Order::getOrderId, order.getOrderId())
            .eq(Order::getUserId, userId)
            .eq(Order::getPayStatus, PayConstants.NOT_CONSUMED)
            .set(Order::getPayStatus, PayConstants.CONSUMED)
            .set(Order::getHours, hours)) > 0;
        if (!ok) {
            throw new ServiceException("操作失败，请重试");
        }
        // 仅消费成功时累加一次学习时长
        if (hours > 0) {
            RecordVo recordVo = recordService.queryByUserId(userId);
            if (recordVo != null) {
                RecordBo recordBo = BeanUtil.toBean(recordVo, RecordBo.class);
                double cur = recordVo.getHours() == null ? 0 : recordVo.getHours();
                recordBo.setHours(cur + hours);
                recordService.updateByBo(recordBo);
            }
        }
        return true;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(Order entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除订单
     *
     * @param ids 需要删除的订单主键
     * @return 结果
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 批量过期指定日期前的订单
     *
     * @param date 过期日期
     */
    @Override
    public void expireDailyOrders(LocalDate date) {
        Date expireDate = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        LambdaQueryWrapper<Order> lqw = Wrappers.lambdaQuery();
        lqw.lt(Order::getEndTime, expireDate);
        // 座位订单创建即 NOT_CONSUMED(3)(余额预付即时下单); UNPAID(0) 一并清理
        lqw.and(w -> w.eq(Order::getPayStatus, PayConstants.NOT_CONSUMED)
            .or().eq(Order::getPayStatus, PayConstants.UNPAID));
        Order update = new Order();
        update.setPayStatus(5); // 5-已过期
        baseMapper.update(update, lqw);
    }

    /**
     * 查询用户今日订单
     *
     * @param userId 会员ID
     * @param date 日期
     * @return 订单列表
     */
    @Override
    public List<OrderVo> getTodayOrders(Long userId, LocalDate date) {
        Date start = Date.from(date.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date end = Date.from(date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        LambdaQueryWrapper<Order> lqw = Wrappers.lambdaQuery();
        lqw.eq(Order::getUserId, userId);
        lqw.ge(Order::getStartTime, start);
        lqw.lt(Order::getStartTime, end);
        return baseMapper.selectVoList(lqw);
    }
}

