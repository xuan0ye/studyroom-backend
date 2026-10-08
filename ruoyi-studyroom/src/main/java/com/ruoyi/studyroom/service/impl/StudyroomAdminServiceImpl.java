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
import com.ruoyi.studyroom.domain.DayCardConsumption;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.studyroom.domain.bo.AdminCardGrantBo;
import com.ruoyi.studyroom.domain.bo.AdminCardUpdateBo;
import com.ruoyi.studyroom.domain.bo.AdminLeaveGrantBo;
import com.ruoyi.studyroom.domain.bo.AdminOccupyBo;
import com.ruoyi.studyroom.domain.bo.AdminRefundBo;
import com.ruoyi.studyroom.domain.bo.AdminRevokeBo;
import com.ruoyi.studyroom.domain.bo.AdminSeatReleaseBo;
import com.ruoyi.studyroom.domain.bo.CardUserBo;
import com.ruoyi.studyroom.domain.bo.SeatLockBo;
import com.ruoyi.studyroom.domain.bo.SeatRecordBo;
import com.ruoyi.studyroom.domain.vo.BalanceVo;
import com.ruoyi.studyroom.domain.vo.CardUserVo;
import com.ruoyi.studyroom.domain.vo.OrderVo;
import com.ruoyi.studyroom.domain.vo.SeatLockVo;
import com.ruoyi.studyroom.domain.vo.SeatOccupationVo;
import com.ruoyi.studyroom.mapper.AccessProfileMapper;
import com.ruoyi.studyroom.mapper.BalanceMapper;
import com.ruoyi.studyroom.mapper.CardMapper;
import com.ruoyi.studyroom.mapper.CardUserMapper;
import com.ruoyi.studyroom.mapper.DayCardConsumptionMapper;
import com.ruoyi.studyroom.mapper.OrderMapper;
import com.ruoyi.studyroom.mapper.SeatLockMapper;
import com.ruoyi.studyroom.mapper.SeatRecordMapper;
import com.ruoyi.studyroom.service.IAccessControlService;
import com.ruoyi.studyroom.service.IBalanceService;
import com.ruoyi.studyroom.service.ICardService;
import com.ruoyi.studyroom.service.ICardUserService;
import com.ruoyi.studyroom.service.IDayCardConsumptionService;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
    private final DayCardConsumptionMapper dayCardConsumptionMapper;
    private final OrderMapper orderMapper;
    /** 释放座位时要解除卡的锁座(长期卡冻结剩余天数的逻辑在那边) */
    private final IDayCardConsumptionService dayCardConsumptionService;

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

        // 2.3 该用户"未结束"的占座记录 —— 撤销能不能真的把座位放出来, 全看这一步。
        //     座位图与预约占用检查读的都是 sr_seat_record, 只把锁座置为已取消是不够的。
        //     (2026-10-08 修: 原来只在 roomId 为空时才清理, 且不看时间范围会把历史记录也删掉)
        int releasedRecords = deleteFutureSeatRecords(userId, null, roomId);
        log.info("[后台运营] 按用户撤销并释放座位: 用户={} 占座记录清理={} 条", userId, releasedRecords);

        return true;
    }

    // ==================== 释放座位 ====================

    @Override
    public List<SeatOccupationVo> listOccupations(Long roomId) {
        List<SeatOccupationVo> list = seatRecordMapper.selectOccupations(roomId);
        if (list == null) {
            return new ArrayList<>();
        }
        // 占用来源: 由关联数据判定, 让后台一眼看出"这个座位是被什么占着"
        for (SeatOccupationVo vo : list) {
            if (vo.getLockId() != null) {
                vo.setSource("代占座/手动锁座");
            } else if (vo.getCardUserId() != null) {
                vo.setSource(StringUtils.isBlank(vo.getCardTitle()) ? "卡锁座" : ("卡锁座(" + vo.getCardTitle() + ")"));
            } else if (vo.getConsumptionId() != null) {
                vo.setSource("日卡预约");
            } else {
                vo.setSource("其它");
            }
        }
        return list;
    }

    /**
     * 取出该用户"未结束"的占座记录(end_time &gt; 现在)—— 只动还没有结束的占用, 历史记录保留。
     */
    private List<SeatRecord> pickFutureSeatRecords(Long userId, Long seatId, Long roomId) {
        LambdaQueryWrapper<SeatRecord> rw = Wrappers.lambdaQuery();
        rw.eq(SeatRecord::getUserId, userId)
            .gt(SeatRecord::getEndTime, new Date())
            .eq(seatId != null, SeatRecord::getSeatId, seatId)
            .eq(roomId != null, SeatRecord::getRoomId, roomId)
            .orderByAsc(SeatRecord::getStartTime);
        return seatRecordMapper.selectList(rw);
    }

    /** 删除该用户"未结束"的占座记录, 返回删除条数 */
    private int deleteFutureSeatRecords(Long userId, Long seatId, Long roomId) {
        List<SeatRecord> records = pickFutureSeatRecords(userId, seatId, roomId);
        if (records == null || records.isEmpty()) {
            return 0;
        }
        List<Long> ids = new ArrayList<>();
        for (SeatRecord r : records) {
            ids.add(r.getId());
        }
        return seatRecordMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> releaseSeat(AdminSeatReleaseBo bo) {
        if (bo == null) {
            throw new ServiceException("释放参数不完整");
        }
        String scope = StringUtils.isBlank(bo.getScope()) ? "seatUser" : bo.getScope();
        Long userId = bo.getUserId();
        Long seatId = bo.getSeatId();
        Long roomId = bo.getRoomId();

        // ---- 1. 定位目标: 占座记录 / 锁座记录 / 用户, 三选一 ----
        if (bo.getSeatRecordId() != null) {
            SeatRecord r = seatRecordMapper.selectById(bo.getSeatRecordId());
            if (r == null) {
                throw new ServiceException("占座记录不存在: " + bo.getSeatRecordId());
            }
            userId = r.getUserId();
            seatId = r.getSeatId();
            if (roomId == null) {
                roomId = r.getRoomId();
            }
        } else if (bo.getLockId() != null) {
            SeatLock lock = seatLockMapper.selectById(bo.getLockId());
            if (lock == null) {
                throw new ServiceException("锁座记录不存在: " + bo.getLockId());
            }
            userId = lock.getUserId();
            seatId = lock.getSeatId();
            if (roomId == null) {
                roomId = lock.getRoomId();
            }
        }
        if (userId == null) {
            throw new ServiceException("释放参数不完整: 需要 占座记录ID / 锁座记录ID / 用户ID 之一");
        }

        // ---- 2. 选出要删的占座记录 ----
        List<SeatRecord> records;
        if ("record".equals(scope)) {
            if (bo.getSeatRecordId() == null) {
                throw new ServiceException("按单条释放时必须提供占座记录ID");
            }
            SeatRecord one = seatRecordMapper.selectById(bo.getSeatRecordId());
            records = new ArrayList<>();
            if (one != null) {
                records.add(one);
            }
        } else {
            records = pickFutureSeatRecords(userId, seatId, roomId);
        }
        // 记下覆盖的日期范围, 用于作废对应的日卡消费记录
        Date minStart = null;
        Date maxEnd = null;
        List<Long> recordIds = new ArrayList<>();
        for (SeatRecord r : records) {
            recordIds.add(r.getId());
            if (r.getStartTime() != null && (minStart == null || r.getStartTime().before(minStart))) {
                minStart = r.getStartTime();
            }
            if (r.getEndTime() != null && (maxEnd == null || r.getEndTime().after(maxEnd))) {
                maxEnd = r.getEndTime();
            }
        }

        // ---- 3. 删占座记录(座位图/预约占用检查读的就是这张表, 删掉座位才真的空出来) ----
        int recordsDeleted = 0;
        if (!recordIds.isEmpty()) {
            recordsDeleted = seatRecordMapper.deleteBatchIds(recordIds);
        }

        // ---- 4. 取消锁座记录(状态置 3=已取消) ----
        int locksCancelled = 0;
        LambdaQueryWrapper<SeatLock> lw = Wrappers.lambdaQuery();
        lw.eq(SeatLock::getUserId, userId)
            .in(SeatLock::getStatus, Arrays.asList(0, 1))
            .eq(seatId != null, SeatLock::getSeatId, seatId)
            .eq(roomId != null, SeatLock::getRoomId, roomId);
        List<SeatLock> locks = seatLockMapper.selectList(lw);
        List<Long> lockIds = new ArrayList<>();
        for (SeatLock l : locks) {
            lockIds.add(l.getId());
        }
        if (bo.getLockId() != null && !lockIds.contains(bo.getLockId())) {
            lockIds.add(bo.getLockId());
        }
        if (!lockIds.isEmpty()) {
            for (Long id : lockIds) {
                SeatLockBo update = new SeatLockBo();
                update.setId(id);
                update.setStatus(3);
                update.setUpdateTime(new Date());
                seatLockService.updateByBo(update);
                locksCancelled++;
            }
        }

        // ---- 5. 解除卡锁座(可选): 长期卡会冻结剩余天数, 不占请假次数 ----
        boolean clearCardLock = bo.getClearCardLock() == null || bo.getClearCardLock();
        String cardMsg = null;
        if (clearCardLock && seatId != null) {
            List<CardUser> cards = cardUserMapper.selectList(Wrappers.<CardUser>lambdaQuery()
                .eq(CardUser::getUserId, userId)
                .eq(CardUser::getLockSeatId, seatId));
            for (CardUser cu : cards) {
                String msg = dayCardConsumptionService.releaseCardLockByAdmin(cu.getId());
                if (StringUtils.isNotBlank(msg)) {
                    cardMsg = msg;
                }
            }
        }

        // ---- 6. 作废对应的日卡消费记录(与小程序「取消预约」同口径: 该天不计入已用) ----
        int consumptionsVoided = 0;
        if (minStart != null && maxEnd != null) {
            LambdaQueryWrapper<DayCardConsumption> cw = Wrappers.lambdaQuery();
            cw.eq(DayCardConsumption::getUserId, userId)
                .eq(seatId != null, DayCardConsumption::getSeatId, seatId)
                .ne(DayCardConsumption::getStatus, 2)
                .ge(DayCardConsumption::getConsumptionDate, DateUtil.beginOfDay(minStart))
                .lt(DayCardConsumption::getConsumptionDate, DateUtil.beginOfDay(maxEnd));
            List<DayCardConsumption> cons = dayCardConsumptionMapper.selectList(cw);
            if (cons != null) {
                for (DayCardConsumption d : cons) {
                    dayCardConsumptionMapper.deleteById(d.getId());
                }
                consumptionsVoided = cons.size();
            }
        }

        // ---- 7. 撤销门禁权限(可选) ----
        boolean revokeAccess = bo.getRevokeAccess() == null || bo.getRevokeAccess();
        boolean accessRevoked = false;
        if (revokeAccess) {
            try {
                accessControlService.revokeAccessByOrder(null, userId, roomId);
                accessRevoked = true;
            } catch (Exception e) {
                log.warn("[后台释放] 撤销门禁失败 userId={} roomId={}", userId, roomId, e);
            }
        }

        StringBuilder msg = new StringBuilder("已释放座位: 删除占座记录 ").append(recordsDeleted).append(" 条")
            .append(", 取消锁座 ").append(locksCancelled).append(" 条");
        if (consumptionsVoided > 0) {
            msg.append(", 作废日卡消费 ").append(consumptionsVoided).append(" 条");
        }
        if (cardMsg != null) {
            msg.append(", ").append(cardMsg);
        }
        if (accessRevoked) {
            msg.append(", 已撤销门禁");
        }
        log.info("[后台释放] 用户={} 座位={} 范围={} 原因={} -> {}", userId, seatId, scope, bo.getReason(), msg);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("recordsDeleted", recordsDeleted);
        out.put("locksCancelled", locksCancelled);
        out.put("consumptionsVoided", consumptionsVoided);
        out.put("cardLockMessage", cardMsg);
        out.put("accessRevoked", accessRevoked);
        out.put("message", msg.toString());
        return out;
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
        // 停用判断看 status(1=启用/0=停用); usable 是区域标记(0=舒适区/1=经济区), 不能当停用状态用
        if (card.getStatus() != null && card.getStatus() == 0) {
            throw new ServiceException("该卡已停用, 不能发放: " + card.getCardTitle());
        }

        Date expiry = bo.getExpiryTime();
        // 到期时间留空 = 不预设到期日: 与小程序新规则一致(客户首次预约使用时才开始计时, 买来放着不会过期)

        CardUserBo add = new CardUserBo();
        add.setUserId(bo.getUserId());
        add.setCardId(bo.getCardId());
        if (expiry != null) {
            add.setExpiryTime(expiry);
        }
        add.setLeaveCount(0);
        add.setLeaveQuota(bo.getLeaveQuota() == null || bo.getLeaveQuota() < 0
            ? ICardUserService.DEFAULT_LEAVE_QUOTA : bo.getLeaveQuota());
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
        if (bo.getLeaveQuota() != null) {
            update.setLeaveQuota(bo.getLeaveQuota());
        }
        if (bo.getLeaveQuotaAdd() != null && bo.getLeaveQuotaAdd() != 0) {
            int base = vo.getLeaveQuota() == null ? ICardUserService.DEFAULT_LEAVE_QUOTA : vo.getLeaveQuota();
            update.setLeaveQuota(Math.max(0, base + bo.getLeaveQuotaAdd()));
        }
        if (update.getExpiryTime() == null && update.getLeaveCount() == null && update.getLeaveQuota() == null) {
            throw new ServiceException("没有需要修改的内容");
        }
        cardUserMapper.updateById(update);
        log.info("[后台运营] 修改卡: 记录={} 用户={} 新到期={} 已请假={} 请假额度={} 说明={}",
            bo.getCardUserId(), vo.getUserId(), bo.getExpiryTime(), bo.getLeaveCount(), update.getLeaveQuota(), bo.getReason());
        return true;
    }

    // ==================== 发放请假次数 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String grantLeave(AdminLeaveGrantBo bo) {
        if (bo == null || bo.getCardUserId() == null) {
            throw new ServiceException("发放请假次数需要传入用户卡记录ID");
        }
        if (bo.getCount() == null || bo.getCount() <= 0) {
            throw new ServiceException("追加次数必须为正整数");
        }
        if (bo.getCount() > 99) {
            throw new ServiceException("单次追加次数不能超过 99");
        }
        CardUserVo vo = cardUserService.queryById(bo.getCardUserId());
        if (vo == null) {
            throw new ServiceException("用户卡记录不存在: " + bo.getCardUserId());
        }
        int base = vo.getLeaveQuota() == null ? ICardUserService.DEFAULT_LEAVE_QUOTA : vo.getLeaveQuota();
        int used = vo.getLeaveCount() == null ? 0 : vo.getLeaveCount();
        int newQuota = base + bo.getCount();
        CardUser update = new CardUser();
        update.setId(bo.getCardUserId());
        update.setLeaveQuota(newQuota);
        if (vo.getLeaveCount() == null) {
            update.setLeaveCount(0);
        }
        cardUserMapper.updateById(update);
        log.info("[后台运营] 发放请假次数: 记录={} 用户={} 原额度={} 追加={} 新额度={} 已用={} 说明={}",
            bo.getCardUserId(), vo.getUserId(), base, bo.getCount(), newQuota, used, bo.getReason());
        return "已发放请假次数 " + bo.getCount() + " 次，该卡当前额度 " + newQuota + " 次（已用 " + used + " 次，剩余 "
            + Math.max(0, newQuota - used) + " 次）";
    }

    // ==================== 删除会员卡券 ====================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String revokeCard(Long cardUserId, String reason) {
        if (cardUserId == null) {
            throw new ServiceException("删除卡券需要传入用户卡记录ID");
        }
        CardUserVo vo = cardUserService.queryById(cardUserId);
        if (vo == null) {
            throw new ServiceException("该卡券不存在或已被删除: " + cardUserId);
        }
        Card card = vo.getCardId() == null ? null : cardMapper.selectById(vo.getCardId());
        String cardTitle = card == null ? ("卡" + vo.getCardId()) : card.getCardTitle();
        Long userId = vo.getUserId();

        // 1) 释放锁座: 删除该卡锁定期内尚未结束的座位占用 + 清空锁座字段
        //    (不清理的话座位会一直被占着, 别人约不了 => 之前 3 号座位那种"孤儿占座")
        int releasedSeats = 0;
        if (vo.getLockSeatId() != null) {
            Date now = new Date();
            List<SeatRecord> records = seatRecordMapper.selectList(Wrappers.<SeatRecord>lambdaQuery()
                .eq(SeatRecord::getSeatId, vo.getLockSeatId())
                .eq(SeatRecord::getUserId, userId));
            if (records != null) {
                for (SeatRecord r : records) {
                    if (r.getEndTime() == null || !r.getEndTime().before(now)) {
                        seatRecordMapper.deleteById(r.getId());
                        releasedSeats++;
                    }
                }
            }
            cardUserService.clearLockSeat(cardUserId);
        }

        // 2) 作废该卡未取消的预约记录(状态 2=已失效)
        int invalidated = dayCardConsumptionMapper.update(null, Wrappers.<DayCardConsumption>lambdaUpdate()
            .eq(DayCardConsumption::getCardUserId, cardUserId)
            .ne(DayCardConsumption::getStatus, 2)
            .set(DayCardConsumption::getStatus, 2));

        // 3) 撤销门禁权限(失败不阻断删除)
        try {
            accessControlService.revokeAccessByOrder(null, userId, null);
        } catch (Exception e) {
            log.warn("[后台运营] 删除卡券时撤销门禁失败: cardUserId={}, userId={}", cardUserId, userId, e);
        }

        // 4) 删除卡券记录
        cardUserMapper.deleteById(cardUserId);
        log.info("[后台运营] 删除卡券: 记录={} 用户={} 卡={} 释放占座={} 作废预约={} 原因={}",
            cardUserId, userId, cardTitle, releasedSeats, invalidated, reason);
        StringBuilder msg = new StringBuilder("已删除「").append(cardTitle).append("」");
        if (releasedSeats > 0) {
            msg.append("，释放座位占用 ").append(releasedSeats).append(" 条");
        }
        if (invalidated > 0) {
            msg.append("，作废预约记录 ").append(invalidated).append(" 条");
        }
        return msg.append("，门禁权限已同步撤销").toString();
    }
}
