package com.ruoyi.studyroom.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.studyroom.domain.AccessProfile;
import com.ruoyi.studyroom.domain.Balance;
import com.ruoyi.studyroom.domain.Card;
import com.ruoyi.studyroom.domain.CardUser;
import com.ruoyi.studyroom.domain.Order;
import com.ruoyi.studyroom.domain.SeatLock;
import com.ruoyi.studyroom.domain.SeatRecord;
import com.ruoyi.studyroom.domain.bo.AdminCardGrantBo;
import com.ruoyi.studyroom.domain.bo.AdminCardUpdateBo;
import com.ruoyi.studyroom.domain.bo.AdminOccupyBo;
import com.ruoyi.studyroom.domain.bo.AdminRefundBo;
import com.ruoyi.studyroom.domain.bo.AdminRevokeBo;
import com.ruoyi.studyroom.domain.bo.CardUserBo;
import com.ruoyi.studyroom.domain.bo.SeatLockBo;
import com.ruoyi.studyroom.domain.bo.SeatRecordBo;
import com.ruoyi.studyroom.domain.vo.BalanceVo;
import com.ruoyi.studyroom.domain.vo.CardUserVo;
import com.ruoyi.studyroom.domain.vo.OrderVo;
import com.ruoyi.studyroom.domain.vo.SeatLockVo;
import com.ruoyi.studyroom.mapper.AccessProfileMapper;
import com.ruoyi.studyroom.mapper.BalanceMapper;
import com.ruoyi.studyroom.mapper.CardMapper;
import com.ruoyi.studyroom.mapper.CardUserMapper;
import com.ruoyi.studyroom.mapper.OrderMapper;
import com.ruoyi.studyroom.mapper.SeatLockMapper;
import com.ruoyi.studyroom.mapper.SeatRecordMapper;
import com.ruoyi.studyroom.service.IAccessControlService;
import com.ruoyi.studyroom.service.IBalanceService;
import com.ruoyi.studyroom.service.ICardService;
import com.ruoyi.studyroom.service.ICardUserService;
import com.ruoyi.studyroom.service.IOrderService;
import com.ruoyi.studyroom.service.ISeatLockService;
import com.ruoyi.studyroom.service.ISeatRecordService;
import com.ruoyi.studyroom.service.IStudyroomAdminService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;

/**
 * 自习室后台运营操作Service实现
 *
 * @author studyroom
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudyroomAdminServiceImpl implements IStudyroomAdminService {

    private final ISeatRecordService seatRecordService;
    private final ISeatLockService seatLockService;
    private final IAccessControlService accessControlService;
    private final IOrderService orderService;
    private final IBalanceService balanceService;
    private final ICardService cardService;
    private final ICardUserService cardUserService;
    private final SeatRecordMapper seatRecordMapper;
    private final SeatLockMapper seatLockMapper;
    private final AccessProfileMapper accessProfileMapper;
    private final BalanceMapper balanceMapper;
    private final CardMapper cardMapper;
    private final CardUserMapper cardUserMapper;
    private final OrderMapper orderMapper;

    // ==================== 代占座/锁座 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean occupy(AdminOccupyBo bo) {
        if (bo == null || bo.getUserId() == null || bo.getSeatId() == null || bo.getRoomId() == null) {
            throw new ServiceException("代占座参数不完整: 需要用户ID/座位ID/自习室ID");
        }
        Date now = new Date();
        Date start = bo.getStartTime() != null ? bo.getStartTime() : DateUtil.beginOfDay(now);
        Date end = bo.getEndTime() != null ? bo.getEndTime() : DateUtil.offsetDay(start, 1);
        if (end.before(start)) {
            throw new ServiceException("结束时间不能早于开始时间");
        }

        // 1. 写占座记录 sr_seat_record
        SeatRecordBo recordBo = new SeatRecordBo();
        recordBo.setUserId(bo.getUserId());
        recordBo.setSeatId(bo.getSeatId());
        recordBo.setRoomId(bo.getRoomId());
        recordBo.setStartTime(start);
        recordBo.setEndTime(end);
        seatRecordService.insertByBo(recordBo);
        log.info("[后台运营] 代占座: 用户={} 座位={} 时间={}~{}", bo.getUserId(), bo.getSeatId(), start, end);

        // 2. 可选: 创建锁座记录 sr_seat_lock
        if (Boolean.TRUE.equals(bo.getCreateLock()) || (bo.getCreateLock() != null && bo.getCreateLock() == 1)) {
            SeatLockBo lockBo = new SeatLockBo();
            lockBo.setUserId(bo.getUserId());
            lockBo.setSeatId(bo.getSeatId());
            lockBo.setRoomId(bo.getRoomId());
            lockBo.setLockType(1); // 手动锁定
            Date lockStart = parseDate(bo.getLockStartDate(), DateUtil.beginOfDay(now));
            Date lockEnd = parseDate(bo.getLockEndDate(), DateUtil.offsetDay(lockStart, 30));
            lockBo.setStartDate(lockStart);
            lockBo.setEndDate(lockEnd);
            lockBo.setStatus(1); // 激活
            lockBo.setAutoRenew(0);
            seatLockService.insertByBo(lockBo);
            log.info("[后台运营] 创建锁座: 用户={} 座位={} 日期={}~{}", bo.getUserId(), bo.getSeatId(), lockStart, lockEnd);
        }

        return true;
    }

    /**
     * 解析 yyyy-MM-dd 字符串为日期, 解析失败返回缺省值
     */
    private Date parseDate(String s, Date defaultVal) {
        if (StringUtils.isBlank(s)) {
            return defaultVal;
        }
        try {
            return DateUtil.parse(s, "yyyy-MM-dd");
        } catch (Exception e) {
            return defaultVal;
        }
    }

    // ==================== 撤销权限 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean revoke(AdminRevokeBo bo) {
        if (bo == null || (bo.getLockId() == null && bo.getUserId() == null)) {
            throw new ServiceException("撤销权限参数不完整: 需要锁座记录ID或用户ID");
        }
        Date now = new Date();

        // 1. 按锁座记录ID撤销: 锁定记录置为已取消 + 撤销该用户该座位的门禁权限
        if (bo.getLockId() != null) {
            SeatLockVo lockVo = seatLockService.queryById(bo.getLockId());
            if (lockVo == null) {
                throw new ServiceException("锁座记录不存在: " + bo.getLockId());
            }
            SeatLockBo update = new SeatLockBo();
            update.setId(bo.getLockId());
            update.setStatus(3); // 已取消
            update.setUpdateTime(now);
            seatLockService.updateByBo(update);
            log.info("[后台运营] 撤销锁座: id={} 用户={} 座位={}", bo.getLockId(), lockVo.getUserId(), lockVo.getSeatId());

            // 撤销门禁权限(该用户的座位授权)
            accessControlService.revokeAccessByOrder(null, lockVo.getUserId(), lockVo.getRoomId());
            return true;
        }

        // 2. 按用户ID撤销: 撤销该用户全部有效锁座记录 + 门禁权限
        Long userId = bo.getUserId();
        Long roomId = bo.getRoomId();

        // 2.1 撤销锁座记录(status != 3)
        LambdaQueryWrapper<SeatLock> lockWrapper = Wrappers.lambdaQuery();
        lockWrapper.eq(SeatLock::getUserId, userId)
            .ne(SeatLock::getStatus, 3)
            .eq(roomId != null, SeatLock::getRoomId, roomId);
        List<SeatLock> locks = seatLockMapper.selectList(lockWrapper);
        for (SeatLock lock : locks) {
            SeatLockBo update = new SeatLockBo();
            update.setId(lock.getId());
            update.setStatus(3);
            update.setUpdateTime(now);
            seatLockService.updateByBo(update);
        }
        log.info("[后台运营] 按用户撤销锁座: 用户={} 记录数={}", userId, locks.size());

        // 2.2 撤销门禁权限
        accessControlService.revokeAccessByOrder(null, userId, roomId);

        // 2.3 该用户当日的占座记录(del_flag 逻辑删)
        if (roomId == null) {
            LambdaQueryWrapper<SeatRecord> recordWrapper = Wrappers.lambdaQuery();
            recordWrapper.eq(SeatRecord::getUserId, userId);
            List<SeatRecord> records = seatRecordMapper.selectList(recordWrapper);
            for (SeatRecord r : records) {
                seatRecordMapper.deleteById(r.getId());
            }
            log.info("[后台运营] 按用户清理占座记录: 用户={} 记录数={}", userId, records.size());
        }

        return true;
    }

    // ==================== 退款 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean refund(AdminRefundBo bo) {
        if (bo == null || (bo.getOrderId() == null && StringUtils.isBlank(bo.getOrderNo()))) {
            throw new ServiceException("退款参数不完整: 需要订单ID或订单号");
        }
        // 查询订单
        Order order = null;
        if (bo.getOrderId() != null) {
            order = orderMapper.selectById(bo.getOrderId());
        } else if (StringUtils.isNotBlank(bo.getOrderNo())) {
            LambdaQueryWrapper<Order> lqw = Wrappers.lambdaQuery();
            lqw.eq(Order::getOrderId, bo.getOrderNo());
            order = orderMapper.selectOne(lqw);
        }
        if (order == null) {
            throw new ServiceException("订单不存在");
        }
        if (order.getUserId() == null) {
            throw new ServiceException("订单缺少用户信息, 无法退款");
        }

        // 已退款状态检查(order_status: PENDING/CONFIRMED/...; pay_status 1-已支付)
        if ("REFUNDED".equalsIgnoreCase(order.getOrderStatus())) {
            throw new ServiceException("该订单已退款");
        }

        // 退款金额: 缺省=订单总额(total 单位分/点?)
        BigDecimal refundAmount = bo.getAmount();
        if (refundAmount == null) {
            // sr_order.total 为整数(单位与余额一致=点)
            refundAmount = order.getTotal() == null ? BigDecimal.ZERO : BigDecimal.valueOf(order.getTotal());
        }
        if (refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ServiceException("退款金额必须大于0");
        }

        // 退款到用户余额(单位: 点)
        addBalance(order.getUserId(), refundAmount);

        // 订单标记为已退款
        Order update = new Order();
        update.setId(order.getId());
        update.setOrderStatus("REFUNDED");
        orderMapper.updateById(update);
        log.info("[后台运营] 退款: 订单={} 用户={} 金额={}点 原因={}", order.getId(), order.getUserId(), refundAmount, bo.getReason());

        // 同时撤销该订单关联的门禁权限
        accessControlService.revokeAccessByOrder(order.getOrderId(), order.getUserId(), order.getRoomId());

        return true;
    }

    /**
     * 给用户余额增加(点)
     */
    private void addBalance(Long userId, BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            return;
        }
        BalanceVo vo = balanceService.queryByUserId(userId);
        long add = amount.setScale(0, RoundingMode.CEILING).longValue();
        if (add <= 0) {
            return;
        }
        if (vo != null && vo.getId() != null) {
            Balance update = new Balance();
            update.setId(vo.getId());
            Long cur = vo.getBalance();
            long next = (cur == null ? 0L : cur) + add;
            update.setBalance(next);
            balanceMapper.updateById(update);
        } else {
            // 无余额记录则新建
            Balance addRec = new Balance();
            addRec.setUserId(userId);
            addRec.setBalance(add);
            balanceMapper.insert(addRec);
        }
    }

    // ==================== 特殊卡发放 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean grantCard(AdminCardGrantBo bo) {
        if (bo == null || bo.getUserId() == null || bo.getCardId() == null) {
            throw new ServiceException("发卡参数不完整: 需要用户ID/卡ID");
        }
        Card card = cardMapper.selectById(bo.getCardId());
        if (card == null) {
            throw new ServiceException("卡不存在: " + bo.getCardId());
        }
        if (card.getUsable() != null && card.getUsable() == 0) {
            throw new ServiceException("该卡已停用, 不能发放: " + card.getCardTitle());
        }

        Date now = new Date();
        Date expiry = bo.getExpiryTime();
        if (expiry == null) {
            // 缺省按卡的 term 天数计算
            long term = card.getTerm() == null ? 30 : card.getTerm();
            expiry = DateUtil.offsetDay(now, (int) term);
        }

        CardUserBo add = new CardUserBo();
        add.setUserId(bo.getUserId());
        add.setCardId(bo.getCardId());
        add.setExpiryTime(expiry);
        add.setLeaveCount(0);
        cardUserService.insertByBo(add);
        log.info("[后台运营] 发卡: 用户={} 卡={}({}) 到期={} 说明={}", bo.getUserId(), bo.getCardId(), card.getCardTitle(), expiry, bo.getReason());
        return true;
    }

    // ==================== 特殊卡修改 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean updateCard(AdminCardUpdateBo bo) {
        if (bo == null || bo.getCardUserId() == null) {
            throw new ServiceException("修改卡参数不完整: 需要用户卡记录ID");
        }
        CardUserVo vo = cardUserService.queryById(bo.getCardUserId());
        if (vo == null) {
            throw new ServiceException("用户卡记录不存在: " + bo.getCardUserId());
        }
        CardUser update = new CardUser();
        update.setId(bo.getCardUserId());
        if (bo.getExpiryTime() != null) {
            update.setExpiryTime(bo.getExpiryTime());
        }
        if (bo.getLeaveCount() != null) {
            update.setLeaveCount(bo.getLeaveCount());
        }
        if (update.getExpiryTime() == null && update.getLeaveCount() == null) {
            throw new ServiceException("没有需要修改的内容");
        }
        cardUserMapper.updateById(update);
        log.info("[后台运营] 修改卡: 记录={} 用户={} 新到期={} 新离开次数={} 说明={}",
            bo.getCardUserId(), vo.getUserId(), bo.getExpiryTime(), bo.getLeaveCount(), bo.getReason());
        return true;
    }
}
