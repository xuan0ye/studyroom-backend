package com.ruoyi.studyroom.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.studyroom.domain.bo.DayCardConsumptionBo;
import com.ruoyi.studyroom.domain.bo.SeatRecordBo;
import com.ruoyi.studyroom.domain.bo.CardUserBo;
import com.ruoyi.studyroom.domain.vo.DayCardConsumptionVo;
import com.ruoyi.studyroom.domain.vo.CardUserVo;
import com.ruoyi.studyroom.domain.vo.SeatRecordVo;
import com.ruoyi.studyroom.domain.vo.SeatVo;
import com.ruoyi.studyroom.domain.DayCardConsumption;
import com.ruoyi.studyroom.mapper.DayCardConsumptionMapper;
import com.ruoyi.studyroom.service.ICardUserService;
import com.ruoyi.studyroom.service.ICardService;
import com.ruoyi.studyroom.service.IDayCardConsumptionService;
import com.ruoyi.studyroom.service.ISeatRecordService;
import com.ruoyi.studyroom.service.ISeatService;
import com.ruoyi.studyroom.service.MiotService;
import com.ruoyi.studyroom.service.IPointsService;
import com.ruoyi.studyroom.service.IBalanceService;
import com.ruoyi.studyroom.service.IUserService;
import com.ruoyi.studyroom.service.IAccessControlService;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 日卡消费记录Service业务层处理
 *
 * @author P_Peaceful
 * @date 2022-05-10
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class DayCardConsumptionServiceImpl implements IDayCardConsumptionService {


    private final DayCardConsumptionMapper baseMapper;
    private final ICardUserService cardUserService;
    private final ICardService cardService;
    private final ISeatRecordService seatRecordService;
    private final ISeatService seatService;
    private final MiotService miotService;
    private final IBalanceService balanceService;
    private final IUserService userService;
    private final IAccessControlService accessControlService;
    private final IPointsService pointsService;    /**
     * 查询日卡消费记录
     *
     * @param id 日卡消费记录主键
     * @return 日卡消费记录
     */
    @Override
    public DayCardConsumptionVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询日卡消费记录列表
     *
     * @param bo 日卡消费记录
     * @return 日卡消费记录
     */
    @Override
    public TableDataInfo<DayCardConsumptionVo> queryPageList(DayCardConsumptionBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<DayCardConsumption> lqw = buildQueryWrapper(bo);
        Page<DayCardConsumptionVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询日卡消费记录列表
     *
     * @param bo 日卡消费记录
     * @return 日卡消费记录
     */
    @Override
    public List<DayCardConsumptionVo> queryList(DayCardConsumptionBo bo) {
        LambdaQueryWrapper<DayCardConsumption> lqw = buildQueryWrapper(bo);
        return baseMapper.selectVoList(lqw);
    }

    private LambdaQueryWrapper<DayCardConsumption> buildQueryWrapper(DayCardConsumptionBo bo) {        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<DayCardConsumption> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getId() != null, DayCardConsumption::getId, bo.getId());
        lqw.eq(bo.getUserId() != null, DayCardConsumption::getUserId, bo.getUserId());
        lqw.eq(bo.getCardUserId() != null, DayCardConsumption::getCardUserId, bo.getCardUserId());
        lqw.eq(bo.getSeatId() != null, DayCardConsumption::getSeatId, bo.getSeatId());
        lqw.eq(bo.getStatus() != null, DayCardConsumption::getStatus, bo.getStatus());
        lqw.ge(bo.getConsumptionDate() != null, DayCardConsumption::getConsumptionDate, bo.getConsumptionDate());
        return lqw;
    }

    /**
     * 后台分页查询座位预约记录(含用户昵称/手机号/座位号/门店/卡券)
     */
    @Override
    public TableDataInfo<DayCardConsumptionVo> queryBookingPage(PageQuery pageQuery, String beginDate, String endDate,
                                                                String nickname, Integer status, Integer seatNum) {
        Page<DayCardConsumptionVo> page = baseMapper.selectBookingPage(pageQuery.build(), beginDate, endDate,
            nickname, status, seatNum);
        return TableDataInfo.build(page);
    }

    /**
     * 新增日卡消费记录
     *
     * @param bo 日卡消费记录
     * @return 结果
     */
    @Override
    public Boolean insertByBo(DayCardConsumptionBo bo) {
        DayCardConsumption add = BeanUtil.toBean(bo, DayCardConsumption.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改日卡消费记录
     *
     * @param bo 日卡消费记录
     * @return 结果
     */
    @Override
    public Boolean updateByBo(DayCardConsumptionBo bo) {
        DayCardConsumption update = BeanUtil.toBean(bo, DayCardConsumption.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(DayCardConsumption entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除日卡消费记录
     *
     * @param ids 需要删除的日卡消费记录主键
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
     * 按天预约座位: 校验日卡有效性、当日重复预约、座位占用, 并写入日卡消费记录与座位占用记录
     *
     * @param bo 日卡消费记录
     * @return 结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean bookByDayCard(DayCardConsumptionBo bo) {
        if (bo.getSeatId() == null) {
            throw new ServiceException("请选择座位");
        }
        if (bo.getCardUserId() == null) {
            throw new ServiceException("请选择日卡");
        }
        // 预约日期: 默认今天, 支持预约未来日期(不超过日卡有效期)
        Date bookingDate;
        if (bo.getConsumptionDate() != null) {
            bookingDate = DateUtil.beginOfDay(bo.getConsumptionDate());
        } else {
            bookingDate = DateUtil.beginOfDay(new Date());
        }
        Date now = new Date();
        if (bookingDate.before(DateUtil.beginOfDay(now))) {
            throw new ServiceException("预约日期不能早于今天");
        }
        // 校验日卡归属与有效期
        CardUserVo cardUser = cardUserService.queryById(bo.getCardUserId());
        if (cardUser == null || !cardUser.getUserId().equals(bo.getUserId())) {
            throw new ServiceException("日卡不存在");
        }
        if (cardUser.getExpiryTime() == null) {
            throw new ServiceException("日卡已过期，请购买新日卡");
        }
        // 卡类型: term=1 日卡(单次), term=7 周卡(7天内每天可约), term>=30 长期卡(期限内每天可约)
        int cardTerm = 1;
        // selectVoById 不带 card 关联, 需单独查卡定义
        com.ruoyi.studyroom.domain.vo.CardVo cardDef = null;
        if (cardUser.getCardId() != null) {
            cardDef = cardService.queryById(cardUser.getCardId());
        }
        if (cardDef != null && cardDef.getTerm() != null) {
            cardTerm = cardDef.getTerm().intValue();
        }
        boolean isDailyCard = cardTerm <= 1;
        // 日卡是否已被使用过(存在消费记录); 周卡/长期卡不按"用过一次就作废"处理
        if (isDailyCard) {
            DayCardConsumptionBo usedBo = new DayCardConsumptionBo();
            usedBo.setCardUserId(bo.getCardUserId());
            List<DayCardConsumptionVo> usedList = queryList(usedBo);
            for (DayCardConsumptionVo d : usedList) {
                if (d.getStatus() == null || d.getStatus() != 2) {
                    throw new ServiceException("该日卡已使用，请选择其他日卡");
                }
            }
        }
        // 自习室24小时营业: 预约覆盖全天 00:00 - 次日00:00
        Date nextDay = DateUtil.offsetDay(bookingDate, 1);
        Date startTime = DateUtil.beginOfDay(bookingDate);
        Date endTime = DateUtil.beginOfDay(nextDay);
        // ===== 卡类型与可用窗口 =====
        // 日卡(term=1): 仅当天
        // 周期卡(7<=term<30, 如周卡7/双周卡14): 连锁 term 天, **不可停卡**
        // 长期卡(term>=30, 如月卡31/季卡93): 连锁"剩余可用天数", 可停卡冻结/续启
        int term = (cardDef != null && cardDef.getTerm() != null) ? cardDef.getTerm().intValue() : 1;
        boolean isLongTerm = term >= 30;
        int lockDays;
        if (isLongTerm) {
            Integer remain = cardUser.getRemainingDays() == null ? term : cardUser.getRemainingDays();
            // 上一连锁窗口自然结束(期间未停卡) => 视为该期已耗尽, 惰性清零
            if (remain > 0 && cardUser.getLockEndTime() != null && cardUser.getLockSeatId() != null
                && !cardUser.getLockEndTime().after(now)) {
                remain = 0;
                CardUserBo remUpd = new CardUserBo();
                remUpd.setId(cardUser.getId());
                remUpd.setRemainingDays(0);
                cardUserService.updateByBo(remUpd);
            }
            if (remain <= 0) {
                throw new ServiceException("该卡剩余天数已用完，如需继续请购买新卡或联系客服");
            }
            lockDays = remain;
        } else if (term >= 7) {
            // 周卡/双周卡等周期卡: 一次预约连锁整段周期
            lockDays = term;
        } else {
            lockDays = 1;
        }
        // 日/周卡: 预约开始日须仍在卡有效期内
        if (!isLongTerm) {
            Date expiryDay = DateUtil.beginOfDay(cardUser.getExpiryTime());
            if (expiryDay.before(bookingDate)) {
                throw new ServiceException("该卡在所选日期已失效，请更换卡券");
            }
        }
        // 已在锁座中(当前窗口未结束)不可再次预约
        if (lockDays > 1 && cardUser.getLockEndTime() != null && cardUser.getLockEndTime().after(now)) {
            throw new ServiceException("该卡已锁定座位，无需重复预约（锁座至 " +
                cn.hutool.core.date.DateUtil.format(cardUser.getLockEndTime(), "yyyy-MM-dd") + "）");
        }
        // 本人该开始日是否已预约
        Date bookingEnd = DateUtil.offsetDay(bookingDate, 1);
        DayCardConsumptionBo mineBo = new DayCardConsumptionBo();
        mineBo.setUserId(bo.getUserId());
        mineBo.setConsumptionDate(bookingDate);
        List<DayCardConsumptionVo> mineList = queryList(mineBo);
        for (DayCardConsumptionVo d : mineList) {
            if (d.getConsumptionDate() != null && d.getConsumptionDate().before(bookingEnd)
                && (d.getStatus() == null || d.getStatus() != 2)) {
                throw new ServiceException("该日期已预约，请勿重复预约");
            }
        }
        // 座位整窗口占用检查(覆盖日卡/连锁/时租等所有占用来源, 排除本人)
        SeatVo seat = seatService.queryById(bo.getSeatId());
        if (seat == null) {
            throw new ServiceException("座位不存在");
        }
        // 卡的区域限制: card_type 0=不限 / 1=仅沉浸区(16-19号) / 2=禁止沉浸区
        if (cardDef != null && cardDef.getCardType() != null && seat.getSeatArea() != null) {
            int areaLimit = cardDef.getCardType();
            if (areaLimit == 2 && seat.getSeatArea() == 1) {
                throw new ServiceException("该卡不能预约沉浸区（16-19号）座位，请选择其它座位");
            }
            if (areaLimit == 1 && seat.getSeatArea() != 1) {
                throw new ServiceException("该卡仅限预约沉浸区（16-19号）座位");
            }
        }
        Date lockEnd = DateUtil.offsetDay(bookingDate, lockDays);
        SeatRecordBo recordBo = new SeatRecordBo();
        recordBo.setRoomId(seat.getRoomId());
        recordBo.setStartTime(DateUtil.beginOfDay(bookingDate));
        recordBo.setEndTime(DateUtil.beginOfDay(lockEnd));
        List<SeatRecordVo> records = seatRecordService.queryListByTime(recordBo);
        for (SeatRecordVo r : records) {
            if (r.getSeatId() != null && r.getSeatId().equals(bo.getSeatId())
                && (r.getUserId() == null || !r.getUserId().equals(bo.getUserId()))) {
                throw new ServiceException("该座位在所选时段已被他人预约");
            }
        }
        // 写入日卡消费记录(首日)
        bo.setStatus(0);
        bo.setConsumptionDate(bookingDate);
        boolean flag = insertByBo(bo);
        // 写入座位占用记录(锁定期间每天一条, 使座位图显示已占用)
        Date cursor = bookingDate;
        while (cursor.before(lockEnd)) {
            Date curStart = DateUtil.beginOfDay(cursor);
            Date curEnd = DateUtil.beginOfDay(DateUtil.offsetDay(cursor, 1));
            SeatRecordBo sr = new SeatRecordBo();
            sr.setRoomId(seat.getRoomId());
            sr.setSeatId(bo.getSeatId());
            sr.setUserId(bo.getUserId());
            sr.setStartTime(curStart);
            sr.setEndTime(curEnd);
            seatRecordService.insertByBo(sr);
            cursor = DateUtil.offsetDay(cursor, 1);
        }
        // 更新卡用户锁座信息
        if (lockDays > 1) {
            CardUserBo cardUserBo = new CardUserBo();
            cardUserBo.setId(cardUser.getId());
            cardUserBo.setLockSeatId(bo.getSeatId());
            cardUserBo.setLockStartTime(DateUtil.beginOfDay(bookingDate));
            cardUserBo.setLockEndTime(lockEnd);
            // 长期卡: 把卡"有效期"同步为当前锁座窗口结束日(与剩余天数模型一致, 供前端展示)
            if (isLongTerm) {
                cardUserBo.setExpiryTime(lockEnd);
            }
            cardUserService.updateByBo(cardUserBo);
        }
        // 预约当天自动打开座位电源(仅预约日期为今天时开启)
        if (DateUtil.isSameDay(bookingDate, now)) {
            try {
                miotService.turnOnSeatDevices(bo.getSeatId());
            } catch (Exception e) {
                log.error("预约成功后自动开灯失败: seatId={}", bo.getSeatId(), e);
            }
        }
        return flag;
    }

    /**
     * 查询用户预约记录(含座位/门店/卡信息)
     *
     * @param userId 用户ID
     * @return 预约记录集合
     */
    @Override
    public List<DayCardConsumptionVo> queryMyRecords(Long userId) {
        return baseMapper.selectMyRecordList(userId);
    }

    /**
     * 取消预约: 删除消费记录与座位占用记录, 释放座位
     * 退款规则: 日卡(term<=1)/周卡(term=7)取消不退款; 长期卡(term>=30)按剩余天数比例退款(退回余额)
     *
     * @param id        消费记录ID
     * @param userId    当前用户ID
     * @param refundMsg 退款结果描述(有退款时返回, 无退款返回null)
     * @return 取消结果
     */
    /** 是否为长期卡(term>=30): 连锁窗口+停卡冻结/续启模型 */
    private boolean isLongTermCard(CardUserVo cu) {
        try {
            if (cu != null && cu.getCardId() != null) {
                com.ruoyi.studyroom.domain.vo.CardVo cv = cardService.queryById(cu.getCardId());
                if (cv != null && cv.getTerm() != null) {
                    return cv.getTerm().intValue() >= 30;
                }
            }
        } catch (Exception ignore) { }
        Integer r = cu == null ? null : cu.getRemainingDays();
        return r != null && r >= 30;
    }

    /**
     * 停卡冻结剩余天数(锁座中): 剩余 = 窗口结束日 与 "明天0点" 之间的自然日(今天已用不计/已冻结)
     */
    private int freezeRemaining(CardUserVo cu) {
        int left = 0;
        if (cu != null && cu.getLockEndTime() != null) {
            Date nowD = new Date();
            Date nextMid = DateUtil.beginOfDay(DateUtil.offsetDay(nowD, 1));
            long ms = cu.getLockEndTime().getTime() - nextMid.getTime();
            left = (int) Math.max(0, Math.ceil(ms / 86400000.0));
        }
        try {
            CardUserBo upd = new CardUserBo();
            upd.setId(cu.getId());
            upd.setRemainingDays(left);
            cardUserService.updateByBo(upd);
        } catch (Exception e) {
            log.warn("冻结长期卡剩余天数失败 cardUserId={}", cu == null ? null : cu.getId(), e);
        }
        return left;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public String cancelByUser(Long id, Long userId) {
        int pausedLeft = -1;
        DayCardConsumptionVo vo = queryById(id);
        if (vo == null || !vo.getUserId().equals(userId)) {
            throw new ServiceException("预约记录不存在");
        }
        if (vo.getStatus() != null && vo.getStatus() == 1) {
            throw new ServiceException("该预约已使用，无法取消");
        }
        // ===== 防恶意占座(2026-09-12): 预约提交后3小时内可取消, 超过3小时不可取消 =====
        if (vo.getCreateTime() != null) {
            long elapsed = System.currentTimeMillis() - vo.getCreateTime().getTime();
            if (elapsed > 3 * 60 * 60 * 1000L) {
                throw new ServiceException("预约已超过3小时，无法取消。如确有特殊情况，请联系门店客服处理。");
            }
        }
        // 删除座位占用记录(该用户该座位当天)
        SeatRecordBo srBo = new SeatRecordBo();
        srBo.setSeatId(vo.getSeatId());
        srBo.setUserId(userId);
        srBo.setStartTime(DateUtil.beginOfDay(vo.getConsumptionDate()));
        srBo.setEndTime(DateUtil.offsetDay(vo.getConsumptionDate(), 1));
        List<SeatRecordVo> records = seatRecordService.queryList(srBo);
        if (!records.isEmpty()) {
            for (SeatRecordVo r : records) {
                seatRecordService.deleteWithValidByIds(java.util.Collections.singleton(r.getId()), false);
            }
        }
        // 周卡/月卡取消: 若该卡当前锁定的正是此座位, 释放整个锁定期并清空锁座字段
        if (vo.getCardUserId() != null) {
            CardUserVo cancelCardUser = cardUserService.queryById(vo.getCardUserId());
            if (cancelCardUser != null && cancelCardUser.getLockSeatId() != null
                && cancelCardUser.getLockSeatId().equals(vo.getSeatId())) {
                // 删除该卡锁定期内的全部座位占用
                SeatRecordBo lockBo = new SeatRecordBo();
                lockBo.setSeatId(vo.getSeatId());
                lockBo.setUserId(userId);
                List<SeatRecordVo> lockRecords = seatRecordService.queryList(lockBo);
                if (lockRecords != null) {
                    for (SeatRecordVo r : lockRecords) {
                        if (r.getStartTime() != null && r.getStartTime().before(cancelCardUser.getLockEndTime())) {
                            seatRecordService.deleteWithValidByIds(java.util.Collections.singleton(r.getId()), false);
                        }
                    }
                }
                // 长期卡: 取消(停卡)即冻结剩余天数, 防"连锁-取消-再连锁"无限白用
                if (isLongTermCard(cancelCardUser)) {
                    pausedLeft = freezeRemaining(cancelCardUser);
                }
                // 清空卡锁座字段
                cardUserService.clearLockSeat(cancelCardUser.getId());
            }
        }
        // 删除消费记录
        deleteWithValidByIds(java.util.Collections.singleton(id), false);
        // 撤销门禁人脸权限(预约取消后删除设备上的人脸)
        try {
            Long roomId = null;
            if (vo.getSeatId() != null) {
                SeatVo seatVo = seatService.queryById(vo.getSeatId());
                roomId = seatVo != null ? seatVo.getRoomId() : null;
            }
            accessControlService.revokeAccessByOrder(String.valueOf(id), userId, roomId);
        } catch (Exception e) {
            log.warn("取消预约时撤销门禁失败 id={}, userId={}", id, userId, e);
        }
        // ===== 退款已下线(2026-09-08): 线上取消预约一律不自动退余额 =====
        // 退款统一走人工客服, 由管理后台运营审核退款(后台 refund 接口保留给客服使用),
        // 避免"预约→取消"循环套现与前端自定价等资金风险
        if (pausedLeft >= 0) {
            return "已取消并停卡：剩余 " + pausedLeft + " 天已冻结，可在「我的卡券」选日期续用（退款请联系客服）";
        }
        return "取消成功（退款请联系客服办理）";
    }

    /**
     * 月卡请假(停卡): 释放锁定的座位, 卡有效期顺延, 每张月卡最多请假2次
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String leaveByCard(Long cardUserId, Long userId) {
        if (cardUserId == null) {
            throw new ServiceException("参数错误");
        }
        CardUserVo cardUser = cardUserService.queryById(cardUserId);
        if (cardUser == null || !cardUser.getUserId().equals(userId)) {
            throw new ServiceException("卡券不存在");
        }
        // 校验是月卡/长期卡(term>=30)
        com.ruoyi.studyroom.domain.vo.CardVo cardDef = null;
        if (cardUser.getCardId() != null) {
            cardDef = cardService.queryById(cardUser.getCardId());
        }
        int term = (cardDef != null && cardDef.getTerm() != null) ? cardDef.getTerm().intValue() : 1;
        if (term < 30) {
            throw new ServiceException("仅月卡/长期卡支持请假停卡");
        }
        // 停卡不限次数: 剩余天数会冻结保存(见 freezeRemaining), 不浪费; leaveCount 仅作记录
        int leaveCount = cardUser.getLeaveCount() == null ? 0 : cardUser.getLeaveCount();
        // 检查是否有锁座
        if (cardUser.getLockSeatId() == null || cardUser.getLockEndTime() == null
            || !cardUser.getLockEndTime().after(new Date())) {
            throw new ServiceException("当前没有锁定的座位，无需请假");
        }
        Long lockSeatId = cardUser.getLockSeatId();
        Date lockStart = cardUser.getLockStartTime();
        Date lockEnd = cardUser.getLockEndTime();
        // 释放座位: 删除该卡用户在该座位锁定期内的所有座位占用记录
        SeatRecordBo delBo = new SeatRecordBo();
        delBo.setSeatId(lockSeatId);
        delBo.setUserId(userId);
        List<SeatRecordVo> records = seatRecordService.queryList(delBo);
        if (records != null) {
            for (SeatRecordVo r : records) {
                if (r.getStartTime() != null && r.getStartTime().before(lockEnd)) {
                    seatRecordService.deleteWithValidByIds(java.util.Collections.singleton(r.getId()), false);
                }
            }
        }
        // 删除锁定期内的日卡消费记录(标记为已失效, 避免显示待使用)
        DayCardConsumptionBo consBo = new DayCardConsumptionBo();
        consBo.setCardUserId(cardUserId);
        List<DayCardConsumptionVo> consList = queryList(consBo);
        if (consList != null) {
            for (DayCardConsumptionVo d : consList) {
                if (d.getConsumptionDate() != null
                    && d.getConsumptionDate().before(lockEnd)
                    && (d.getStatus() == null || d.getStatus() == 0)) {
                    DayCardConsumptionBo upd = new DayCardConsumptionBo();
                    upd.setId(d.getId());
                    upd.setStatus(2); // 已失效(请假)
                    updateByBo(upd);
                }
            }
        }
        // 更新卡用户: 清空锁座, 停卡/请假计数+1; 长期卡=冻结剩余天数, 其它卡=有效期顺延(兼容旧逻辑)
        CardUserBo cardUserBo = new CardUserBo();
        cardUserBo.setId(cardUser.getId());
        cardUserBo.setLeaveCount(leaveCount + 1);
        String leftMsg;
        if (isLongTermCard(cardUser)) {
            int left = freezeRemaining(cardUser);
            leftMsg = "剩余 " + left + " 天已冻结，可在「我的卡券」再选日期续用";
        } else {
            long remainMs = lockEnd.getTime() - Math.max(new Date().getTime(), lockStart.getTime());
            int remainDays = (int) Math.max(1, Math.ceil(remainMs / 86400000.0));
            Date newExpiry = DateUtil.offsetDay(
                cardUser.getExpiryTime() == null ? new Date() : cardUser.getExpiryTime(), remainDays);
            cardUserBo.setExpiryTime(newExpiry);
            leftMsg = "有效期顺延 " + remainDays + " 天";
        }
        cardUserService.clearLockSeat(cardUser.getId());
        cardUserService.updateByBo(cardUserBo);
        return "停卡成功：" + leftMsg + "（第 " + (leaveCount + 1) + " 次）";
    }

    /**
     * 周卡/月卡取消锁座: 释放锁定的座位并清空锁座字段(不退款, 可重新预约)
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public String unlockByCard(Long cardUserId, Long userId) {
        if (cardUserId == null) {
            throw new ServiceException("参数错误");
        }
        CardUserVo cardUser = cardUserService.queryById(cardUserId);
        if (cardUser == null || !cardUser.getUserId().equals(userId)) {
            throw new ServiceException("卡券不存在");
        }
        // 检查是否有锁座
        if (cardUser.getLockSeatId() == null || cardUser.getLockEndTime() == null
            || !cardUser.getLockEndTime().after(new Date())) {
            throw new ServiceException("当前没有锁定的座位");
        }
        Long lockSeatId = cardUser.getLockSeatId();
        Date lockEnd = cardUser.getLockEndTime();
        // 释放座位: 删除该卡用户在该座位锁定期内的所有座位占用记录
        SeatRecordBo delBo = new SeatRecordBo();
        delBo.setSeatId(lockSeatId);
        delBo.setUserId(userId);
        List<SeatRecordVo> records = seatRecordService.queryList(delBo);
        if (records != null) {
            for (SeatRecordVo r : records) {
                if (r.getStartTime() != null && r.getStartTime().before(lockEnd)) {
                    seatRecordService.deleteWithValidByIds(java.util.Collections.singleton(r.getId()), false);
                }
            }
        }
        // 删除锁定期内的日卡消费记录(该卡产生的预约记录)
        DayCardConsumptionBo consBo = new DayCardConsumptionBo();
        consBo.setCardUserId(cardUserId);
        List<DayCardConsumptionVo> consList = queryList(consBo);
        if (consList != null) {
            for (DayCardConsumptionVo d : consList) {
                if (d.getConsumptionDate() != null
                    && d.getConsumptionDate().before(lockEnd)
                    && (d.getStatus() == null || d.getStatus() == 0)) {
                    DayCardConsumptionBo upd = new DayCardConsumptionBo();
                    upd.setId(d.getId());
                    upd.setStatus(2); // 已失效(取消锁座)
                    updateByBo(upd);
                }
            }
        }
        // 长期卡停卡: 冻结剩余天数(防"连锁-取消-再连锁"无限白用)
        if (isLongTermCard(cardUser)) {
            freezeRemaining(cardUser);
        }
        // 清空卡锁座字段
        cardUserService.clearLockSeat(cardUser.getId());
        if (isLongTermCard(cardUser)) {
            CardUserVo refreshed = cardUserService.queryById(cardUserId);
            int left = refreshed != null && refreshed.getRemainingDays() != null ? refreshed.getRemainingDays() : 0;
            return "已停卡：剩余 " + left + " 天已冻结，可在「我的卡券」选日期续用";
        }
        return "已取消锁座，座位已释放，可重新预约";
    }

    /**
     * 开灯签到: 记录用户当天首次开灯时间(用于连续签到/早到榜)
     * 仅当当天有预约记录(status=0)且尚未签到过时写入, 保留最早时间
     */
    @Override
    public boolean checkInByLightOn(Long seatId, Long userId) {
        if (seatId == null || userId == null) return false;
        Date today = DateUtil.beginOfDay(new Date());
        Date tomorrow = DateUtil.offsetDay(today, 1);
        DayCardConsumptionBo bo = new DayCardConsumptionBo();
        bo.setUserId(userId);
        bo.setSeatId(seatId);
        List<DayCardConsumptionVo> list = queryList(bo);
        for (DayCardConsumptionVo vo : list) {
            if (vo.getConsumptionDate() != null
                && !vo.getConsumptionDate().before(today)
                && vo.getConsumptionDate().before(tomorrow)
                && (vo.getStatus() == null || vo.getStatus() == 0)) {
                // 已签到则不覆盖(保留最早时间)
                if (vo.getCheckInTime() != null) return false;
                DayCardConsumptionBo update = new DayCardConsumptionBo();
                update.setId(vo.getId());
                update.setCheckInTime(new Date());
                update.setStatus(1); // 1-已使用: 开灯即签到=已到店使用, 当天预约不可再取消
                updateByBo(update);
                // 签到成功奖励积分(每天首次签到加分, 按连续天数递增, 已由积分服务去重)
                try {
                    int earned = pointsService.addByCheckIn(userId);
                    if (earned > 0) {
                        log.info("签到奖励积分 userId={} earned={}", userId, earned);
                    }
                } catch (Exception e) {
                    log.warn("签到积分发放失败 userId={}", userId, e);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * 连续签到排行榜: 按连续签到天数降序
     * 签到=当天有开灯记录(check_in_time非空)。连续=从最近一次签到日起往前连续签到不中断。
     */
    @Override
    public List<Map<String, Object>> queryStreakRank() {
        // 取所有有签到的消费记录(userId, consumption_date, check_in_time)
        LambdaQueryWrapper<DayCardConsumption> lqw = Wrappers.lambdaQuery();
        lqw.isNotNull(DayCardConsumption::getCheckInTime);
        lqw.orderByAsc(DayCardConsumption::getConsumptionDate);
        List<DayCardConsumption> all = baseMapper.selectList(lqw);
        // 按用户分组签到日期
        java.util.Map<Long, java.util.List<java.util.Date>> byUser = new java.util.TreeMap<>();
        for (DayCardConsumption c : all) {
            if (c.getConsumptionDate() == null) continue;
            byUser.computeIfAbsent(c.getUserId(), k -> new java.util.ArrayList<>())
                .add(c.getConsumptionDate());
        }
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (java.util.Map.Entry<Long, java.util.List<java.util.Date>> e : byUser.entrySet()) {
            // 去重(同一天可能多次)并排序
            java.util.Set<java.util.Date> days = new java.util.TreeSet<>(e.getValue());
            java.util.List<java.util.Date> sorted = new java.util.ArrayList<>(days);
            int streak = 0;
            // 从今天(或最近签到日)往回数连续天数
            java.util.Date today = DateUtil.beginOfDay(new Date());
            java.util.Date cursor = today;
            if (sorted.isEmpty()) continue;
            java.util.Date last = DateUtil.beginOfDay(sorted.get(sorted.size() - 1));
            // 如果最后签到日早于昨天, 连续中断(从0开始); 若等于今天或昨天, 从该日往前数
            if (last.before(DateUtil.offsetDay(today, -1))) {
                cursor = last; // 中断了, 但仍显示从最后签到日开始的连续(通常为1)
            }
            java.util.Set<String> dayStrs = new java.util.HashSet<>();
            for (java.util.Date d : sorted) dayStrs.add(DateUtil.formatDate(d));
            while (dayStrs.contains(DateUtil.formatDate(cursor))) {
                streak++;
                cursor = DateUtil.offsetDay(cursor, -1);
            }
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("userId", e.getKey());
            item.put("streak", streak);
            item.put("totalDays", days.size());
            item.put("nickname", getNickname(e.getKey()));
            item.put("avatarUrl", getAvatar(e.getKey()));
            result.add(item);
        }
        result.sort((a, b) -> Integer.compare((int) b.get("streak"), (int) a.get("streak")));
        return result;
    }

    /**
     * 今日早到排行榜: 按当天首次开灯时间升序(谁先到谁靠前)
     */
    @Override
    public List<Map<String, Object>> queryEarlyRank() {
        Date today = DateUtil.beginOfDay(new Date());
        Date tomorrow = DateUtil.offsetDay(today, 1);
        LambdaQueryWrapper<DayCardConsumption> lqw = Wrappers.lambdaQuery();
        lqw.ge(DayCardConsumption::getConsumptionDate, today)
            .lt(DayCardConsumption::getConsumptionDate, tomorrow)
            .isNotNull(DayCardConsumption::getCheckInTime);
        List<DayCardConsumption> all = baseMapper.selectList(lqw);
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (DayCardConsumption c : all) {
            if (c.getUserId() == null || c.getCheckInTime() == null) continue;
            if (!seen.add(c.getUserId())) continue; // 每人只算最早一次
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("userId", c.getUserId());
            item.put("checkInTime", c.getCheckInTime());
            item.put("nickname", getNickname(c.getUserId()));
            item.put("avatarUrl", getAvatar(c.getUserId()));
            result.add(item);
        }
        result.sort((a, b) -> ((java.util.Date) a.get("checkInTime")).compareTo((java.util.Date) b.get("checkInTime")));
        return result;
    }

    private String getNickname(Long userId) {
        try {
            com.ruoyi.studyroom.domain.vo.UserVo u = userService.queryById(userId);
            return u != null && u.getNickname() != null ? u.getNickname() : ("用户" + userId);
        } catch (Exception e) {
            return "用户" + userId;
        }
    }

    private String getAvatar(Long userId) {
        try {
            com.ruoyi.studyroom.domain.vo.UserVo u = userService.queryById(userId);
            String avatar = u != null ? u.getAvatarUrl() : "";
            return normalizeAvatar(avatar);
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 头像值归一化: 数据库可能存裸 base64 或 data URI 截断残留(如 /9j/... 前导斜杠),
     * 统一补 data:image 前缀, 避免前端 image 组件把 base64 当 URL 加载报错
     */
    private String normalizeAvatar(String avatar) {
        if (avatar == null || avatar.isEmpty()) {
            return "";
        }
        String s = avatar.trim();
        // 已带 data:image 前缀或为 URL
        if (s.startsWith("data:image") || s.startsWith("http") || s.startsWith("wxfile")) {
            return s;
        }
        // 兼容 data URI 截断残留: 以 / 开头但去掉斜杠后仍是 base64(如 /9j/4AAQ...), 补前缀
        String t = s;
        if (t.startsWith("/")) {
            t = t.substring(1);
        }
        // 裸 base64: 仅由 base64 字符组成且足够长(图片数据), 补 JPEG 前缀
        if (t.length() > 50 && t.matches("[A-Za-z0-9+/=]+")) {
            return "data:image/jpeg;base64," + t;
        }
        // 以 / 开头的本地资源路径原样返回
        if (s.startsWith("/")) {
            return s;
        }
        return s;
    }
}
