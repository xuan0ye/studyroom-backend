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
    private final IPointsService pointsService;

    /**
     * 今日早到榜的起算小时(默认 6 点)。
     *
     * <p>页面文案承诺的是「今日 <b>6 点后</b>，谁先到谁先开灯」，但后端原先只按日期过滤，
     * 于是凌晨 1:20 开灯的人拿了早到榜第一 —— 规矩写了没落实。这里补上真实过滤。</p>
     */
    @org.springframework.beans.factory.annotation.Value("${studyroom.rank.early-start-hour:6}")
    private int earlyStartHour;

    /**
     * 开灯签到开关(<b>2026-10-12 起默认开</b>, yml: {@code studyroom.checkin.light-on-enabled})。
     *
     * <p><b>为什么默认开</b>: "门禁刷脸到店签到"({@link #checkInByDoor}) 在本机硬件上不可能实现 ——
     * 设备不支持服务器主动读通行记录({@code read_record} 回 code 1/2), 刷卡开门也不向我们推送任何报文,
     * 标准协议的 HTTP {@code upRecord} 同样没有。实测 10-05~10-12 签到彻底断流:
     * 排行榜三个榜全空、签到积分不发、"已签到不允许取消"的保护失效。</p>
     *
     * <p>故退回开灯口径(与早到榜文案"今日 6 点后谁先到谁先开灯"一致)。
     * 将来换成会推送通行记录的门禁机, 把环境变量 {@code STUDYROOM_CHECKIN_LIGHT_ON_ENABLED=false}
     * 设上即可切回刷脸口径, 不需要改代码。</p>
     */
    @org.springframework.beans.factory.annotation.Value("${studyroom.checkin.light-on-enabled:true}")
    private boolean lightOnCheckInEnabled;    /**
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
        // 校验日卡归属
        CardUserVo cardUser = cardUserService.queryById(bo.getCardUserId());
        if (cardUser == null || !cardUser.getUserId().equals(bo.getUserId())) {
            throw new ServiceException("日卡不存在");
        }
        // 该卡是否"真正用过": 存在未取消、未删除的消费记录(与前端卡包 consumed 口径完全一致)
        // 取消预约/请假会把记录标记为 status=2, 所以"订了又取消"不算用过
        boolean usedBefore = false;
        List<CardUserVo> myCards = cardUserService.queryListByUserId(bo.getUserId());
        if (myCards != null) {
            for (CardUserVo cu : myCards) {
                if (cu.getId() != null && cu.getId().equals(bo.getCardUserId())) {
                    usedBefore = Boolean.TRUE.equals(cu.getConsumed());
                    break;
                }
            }
        }
        // 从未真正用过的卡(买来一直没用、或预约都被取消): 不过期, 本次预约即为「首次使用」, 有效期从本次起算
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
        // 固定区间卡(如中秋国庆畅享券): 只能在 validStart~validEnd 内使用, 且每天单独预约、不连锁锁座
        boolean fixedWindow = cardDef != null && cardDef.getValidStart() != null && cardDef.getValidEnd() != null;
        Date fixedStart = fixedWindow ? DateUtil.beginOfDay(cardDef.getValidStart()) : null;
        Date fixedEndExclusive = fixedWindow ? DateUtil.offsetDay(DateUtil.beginOfDay(cardDef.getValidEnd()), 1) : null;
        if (fixedWindow && (bookingDate.before(fixedStart) || !bookingDate.before(fixedEndExclusive))) {
            throw new ServiceException("「" + cardDef.getCardTitle() + "」仅限 "
                + cn.hutool.core.date.DateUtil.format(cardDef.getValidStart(), "M月d日") + " 至 "
                + cn.hutool.core.date.DateUtil.format(cardDef.getValidEnd(), "M月d日")
                + " 使用，所选日期不在可用范围内");
        }
        // 日卡用过一次即作废(周卡/长期卡按窗口/剩余天数结算; 固定区间卡每天可约)
        if (isDailyCard && usedBefore && !fixedWindow) {
            throw new ServiceException("该日卡已使用，请选择其他日卡");
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
        if (fixedWindow) {
            // 固定区间卡: 每天单独预约, 一次只锁当天(不连锁锁座)
            lockDays = 1;
        } else if (isLongTerm) {
            Integer remain = cardUser.getRemainingDays() == null ? term : cardUser.getRemainingDays();
            if (!usedBefore) {
                // 首次使用: 以卡面天数起算(防止历史数据里异常的剩余天数)
                remain = Math.min(remain, term);
            } else if (remain > 0 && cardUser.getLockEndTime() != null && cardUser.getLockSeatId() != null
                && !cardUser.getLockEndTime().after(now)) {
                // 上一连锁窗口自然结束(期间未停卡) => 视为该期已耗尽, 惰性清零
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
            // 周卡/双周卡等周期卡: 一次预约连锁整段周期, 窗口不得超过本卡剩余有效期
            lockDays = term;
            if (usedBefore && cardUser.getExpiryTime() != null) {
                long leftMs = DateUtil.beginOfDay(cardUser.getExpiryTime()).getTime() - bookingDate.getTime();
                int left = (int) (leftMs / 86400000L);
                if (left <= 0) {
                    throw new ServiceException("该卡可使用天数已用完，如需继续请购买新卡或联系客服");
                }
                if (left < lockDays) {
                    lockDays = left;
                }
            }
        } else {
            lockDays = 1;
        }
        // 已用过的日/周卡: 预约开始日须仍在有效期内; 从未用过的卡与固定区间卡不设此限(区间已单独校验)
        if (!isLongTerm && !fixedWindow && usedBefore && cardUser.getExpiryTime() != null) {
            Date expiryDay = DateUtil.beginOfDay(cardUser.getExpiryTime());
            if (expiryDay.before(bookingDate)) {
                throw new ServiceException("该卡在所选日期已失效，请更换卡券");
            }
        }
        // 已用过的周期卡(周卡/双周卡): 锁座窗口已结束 => 本卡可使用天数已用完, 不允许再约
        if (!isLongTerm && term >= 7 && usedBefore && cardUser.getLockSeatId() != null
            && cardUser.getLockEndTime() != null && !cardUser.getLockEndTime().after(now)) {
            throw new ServiceException("该卡可使用天数已用完，如需继续请购买新卡或联系客服");
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
        // 卡的区域限制: card_type 0=两区通用 / 1=仅沉浸区(16-19号) / 2=仅经济区
        if (cardDef != null && cardDef.getCardType() != null && seat.getSeatArea() != null) {
            int areaLimit = cardDef.getCardType();
            if (areaLimit == 2 && seat.getSeatArea() == 1) {
                throw new ServiceException("该卡为「仅经济区」，不能预约沉浸区（16-19 号）座位，请换一个座位");
            }
            if (areaLimit == 1 && seat.getSeatArea() != 1) {
                throw new ServiceException("该卡为「仅沉浸区」，只能预约 16-19 号座位");
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
                // 报出具体冲突信息: 记录可能早于本次预约窗口(如门店长期锁座) => 改报"占用至"
                String when;
                if (r.getStartTime() != null && r.getStartTime().before(DateUtil.beginOfDay(bookingDate))) {
                    String endLabel = r.getEndTime() == null ? "更晚"
                        : cn.hutool.core.date.DateUtil.format(r.getEndTime(), "MM月dd日");
                    when = "已被占用（占用至 " + endLabel + "）";
                } else if (r.getStartTime() == null) {
                    when = "在所选时段已被预约";
                } else {
                    when = "在 " + cn.hutool.core.date.DateUtil.format(r.getStartTime(), "MM月dd日") + " 已被预约";
                }
                if (lockDays > 1) {
                    throw new ServiceException("该座位" + when + "；这张卡需要连续锁定 "
                        + lockDays + " 天，请换一个座位或改用日卡");
                }
                throw new ServiceException("该座位" + when + "，请选择其它座位");
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
        // 更新卡用户锁座信息 + 首次使用激活
        CardUserBo cardUserBo = new CardUserBo();
        cardUserBo.setId(cardUser.getId());
        boolean needUpdate = false;
        if (lockDays > 1) {
            cardUserBo.setLockSeatId(bo.getSeatId());
            cardUserBo.setLockStartTime(DateUtil.beginOfDay(bookingDate));
            cardUserBo.setLockEndTime(lockEnd);
            needUpdate = true;
        }
        if (fixedWindow) {
            // 固定区间卡: 有效期固定为区间末次日00:00, 不随预约变动(过期即失效)
            cardUserBo.setExpiryTime(fixedEndExclusive);
            needUpdate = true;
        } else if (cardUser.getExpiryTime() == null || !usedBefore) {
            // 首次使用(或此前预约全部被取消、从未真正用上): 有效期从本次使用起算
            cardUserBo.setExpiryTime(lockEnd);
            needUpdate = true;
        } else if (isLongTerm) {
            // 长期卡: 把卡"有效期"同步为当前锁座窗口结束日(与剩余天数模型一致, 供前端展示)
            cardUserBo.setExpiryTime(lockEnd);
            needUpdate = true;
        }
        if (needUpdate) {
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
        // ===== 取消时间窗(2026-10-03 放宽): 未签到(状态0)且在预约当日 24:00 前均可取消 =====
        // 旧规则是"下单后 3 小时内", 实际导致绝大多数用户根本取消不了;
        // 已开灯签到(status=1)即视为已到店使用, 不允许再取消(防"用完再退");
        // 长期卡(月卡/季卡)过了首日请用卡包里的「请假停卡 / 取消锁座」, 那才是它的正确入口。
        if (vo.getStatus() != null && vo.getStatus() == 2) {
            throw new ServiceException("该预约已失效，无需取消");
        }
        if (vo.getConsumptionDate() != null) {
            Date bookingDayEnd = DateUtil.offsetDay(DateUtil.beginOfDay(vo.getConsumptionDate()), 1);
            if (!new Date().before(bookingDayEnd)) {
                throw new ServiceException("预约日期已过，无法直接取消。月卡/季卡等长期卡请到「我的 → 卡包」里请假停卡或取消锁座。");
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
            throw new ServiceException("仅月卡/季卡等长期卡支持请假停卡");
        }
        // 请假额度校验: 每张卡默认 3 次(后台可单独追加), 用完需联系客服
        int leaveCount = cardUser.getLeaveCount() == null ? 0 : cardUser.getLeaveCount();
        int quota = cardUser.getLeaveQuota() == null ? ICardUserService.DEFAULT_LEAVE_QUOTA : cardUser.getLeaveQuota();
        if (leaveCount >= quota) {
            throw new ServiceException("该卡的请假次数已用完（共 " + quota + " 次）。如确有特殊原因，请联系门店客服为你追加请假次数。");
        }
        // 检查是否有锁座(只有正在用的卡才需要请假停卡)
        if (cardUser.getLockSeatId() == null || cardUser.getLockEndTime() == null
            || !cardUser.getLockEndTime().after(new Date())) {
            throw new ServiceException("该卡当前没有正在使用的座位（尚未开始使用或已停卡），无需请假");
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
        int leftTimes = quota - (leaveCount + 1);
        if (leftTimes < 0) leftTimes = 0;
        return "请假成功：" + leftMsg + "（已请假 " + (leaveCount + 1) + "/" + quota + " 次，还可请假 " + leftTimes + " 次）";
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
        // 长期卡(月卡/季卡)只能走「请假停卡」:
        // 两个入口对长期卡都会 freezeRemaining() + clearLockSeat() + 失效消费记录, 行为完全等价,
        // 若同时开放, 用户点"取消锁座"即可免费达成请假的全部效果 => 请假次数形同虚设。
        // 前端已隐藏该入口, 这里兜底防止直接调 /api/wx/daily/unlock 绕过额度。
        if (isLongTermCard(cardUser)) {
            throw new ServiceException("月卡/季卡等长期卡不支持取消锁座，请使用「请假停卡」（每张卡有请假次数限制）");
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
        // 长期卡已在上方拦截, 走到这里的只有周期卡(周卡/双周卡)。
        // 周期卡的有效期由 expiryTime 决定, 不参与 remainingDays 冻结模型,
        // 释放锁座后有效期照旧, 用户可直接重新预约。
        cardUserService.clearLockSeat(cardUser.getId());
        return "已取消锁座，座位已释放，可重新预约";
    }

    /**
     * 门禁刷脸签到 (取代开灯签到)。
     *
     * <p>与 {@link #checkInByLightOn} 的关键区别:</p>
     * <ul>
     *   <li>不需要 seatId —— 人刚到门口, 可能还没落座;</li>
     *   <li>时间用设备给的通行时间(更准), 设备没给才退回当前时间;</li>
     *   <li><b>不新建记录</b>。新建会牵扯日卡扣次, 代价太大; 找不到当天记录只记日志。</li>
     * </ul>
     */
    @Override
    public boolean checkInByDoor(Long userId, Date eventTime) {
        if (userId == null) {
            return false;
        }
        Date today = DateUtil.beginOfDay(new Date());
        Date tomorrow = DateUtil.offsetDay(today, 1);
        DayCardConsumptionBo bo = new DayCardConsumptionBo();
        bo.setUserId(userId);
        for (DayCardConsumptionVo vo : queryList(bo)) {
            if (vo.getConsumptionDate() == null
                || vo.getConsumptionDate().before(today)
                || !vo.getConsumptionDate().before(tomorrow)) {
                continue;
            }
            Date when = eventTime != null ? eventTime : new Date();
            // 已签到: 只在"新时间更早"时回填(保留当天最早的一次), 否则不动
            if (vo.getCheckInTime() != null) {
                if (!when.before(vo.getCheckInTime())) {
                    return false;
                }
            }
            DayCardConsumptionBo update = new DayCardConsumptionBo();
            update.setId(vo.getId());
            update.setCheckInTime(when);
            update.setStatus(1); // 1-已使用
            boolean ok = updateByBo(update);
            log.info("[到店签到] 门禁刷脸签到 userId={} 时间={} 结果={}", userId, when, ok);
            return ok;
        }
        // 当天还没有消费记录(例如还没开始计次的月卡): 不动数据, 但必须留痕, 便于发现口径缺口
        log.warn("[到店签到] 门禁刷脸到店但当天无消费记录, 未签到 userId={} (如需支持请确认是否该新建记录)", userId);
        return false;
    }

    /**
     * 开灯签到: 记录用户当天首次开灯时间(用于连续签到/早到榜)
     * 仅当当天有预约记录(status=0)且尚未签到过时写入, 保留最早时间
     */
    @Override
    public boolean checkInByLightOn(Long seatId, Long userId) {
        // 当前口径 = 开灯签到(2026-10-12 起默认开), 详见 lightOnCheckInEnabled 的说明。
        // 门禁刷脸口径(checkInByDoor)在本机硬件上收不到任何事件, 已不可用;
        // 将来换了会推送通行记录的门禁机, 把开关关掉即可切回。
        if (!lightOnCheckInEnabled) {
            return false;
        }
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
                awardCheckInPoints(userId);
                return true;
            }
        }
        // 锁座卡(月卡/季卡/锁座中的周卡)在锁座期内不会每天生成消费记录 —— 补一条今天的记录
        if (ensureTodayRecordForLockedSeat(seatId, userId)) {
            awardCheckInPoints(userId);
            return true;
        }
        return false;
    }

    /** 签到成功奖励积分(每天首次加分, 按连续天数递增, 已由积分服务去重) */
    private void awardCheckInPoints(Long userId) {
        try {
            int earned = pointsService.addByCheckIn(userId);
            if (earned > 0) {
                log.info("签到奖励积分 userId={} earned={}", userId, earned);
            }
        } catch (Exception e) {
            log.warn("签到积分发放失败 userId={}", userId, e);
        }
    }

    /**
     * 锁座卡「当天没有消费记录」时, 补一条今天的记录并视为已签到。
     *
     * <p><b>为什么必须补</b>: 锁座期内 {@link #book} 会直接拒绝再次预约
     * ("该卡已锁定座位，无需重复预约"), 所以月卡/季卡/锁座中的周卡同学在锁座期内
     * <b>不会</b>每天生成 {@code sr_day_card_consumption} 记录; 而签到原先要求"当天有记录",
     * 于是这批人到店开灯也签不上 —— 排行榜上永远没有他们
     * (2026-10-12 实测两位季卡/月卡同学, 15:08 与 19:11 点了开灯却无签到)。</p>
     *
     * <p>判定条件与 {@code DeviceController} 放行"开灯"的第 3 条<b>完全一致</b>
     * (本人会员卡 {@code lock_seat_id} = 该座位, 且锁座窗口覆盖今天), 避免"能开灯却签不上"的口径分裂。</p>
     *
     * <p>安全性: 锁座卡的使用天数由<b>锁座窗口</b>决定(见 {@link #freezeRemaining}, 以及预约时
     * "把卡有效期同步为锁座窗口结束日"), 补一条当天记录不会重复扣次, 也不影响请假/停卡逻辑。</p>
     *
     * @return true 表示已补记录并写入签到时间
     */
    private boolean ensureTodayRecordForLockedSeat(Long seatId, Long userId) {
        if (seatId == null || userId == null) {
            return false;
        }
        try {
            CardUserBo cuQuery = new CardUserBo();
            cuQuery.setUserId(userId);
            cuQuery.setLockSeatId(seatId);
            List<CardUserVo> cards = cardUserService.queryList(cuQuery);
            if (cards == null || cards.isEmpty()) {
                return false;
            }
            Date dayStart = DateUtil.beginOfDay(new Date());
            Date dayEnd = DateUtil.offsetDay(dayStart, 1);
            for (CardUserVo cu : cards) {
                if (cu.getLockSeatId() == null || !cu.getLockSeatId().equals(seatId)) {
                    continue;
                }
                if (cu.getLockStartTime() == null || cu.getLockEndTime() == null) {
                    continue;
                }
                if (!cu.getLockStartTime().before(dayEnd) || !cu.getLockEndTime().after(dayStart)) {
                    continue;
                }
                DayCardConsumptionBo add = new DayCardConsumptionBo();
                add.setUserId(userId);
                add.setSeatId(seatId);
                add.setCardUserId(cu.getId());
                add.setConsumptionDate(dayStart);
                add.setStatus(1);
                add.setCheckInTime(new Date());
                Boolean ok = insertByBo(add);
                if (Boolean.TRUE.equals(ok)) {
                    log.info("[到店签到] 锁座卡补当天记录并签到: userId={} cardUserId={} seatId={}",
                        userId, cu.getId(), seatId);
                    return true;
                }
                log.warn("[到店签到] 锁座卡补记录未成功 userId={} cardUserId={}", userId, cu.getId());
            }
        } catch (Exception e) {
            log.warn("[到店签到] 锁座卡补记录失败 userId={} seatId={}: {}", userId, seatId, e.getMessage());
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
            if (sorted.isEmpty()) continue;
            java.util.Date today = DateUtil.beginOfDay(new Date());
            java.util.Date yesterday = DateUtil.offsetDay(today, -1);
            java.util.Date last = DateUtil.beginOfDay(sorted.get(sorted.size() - 1));
            // 只有"最后签到日是今天或昨天"才算连续仍在进行中。
            // 原代码只在 last 早于昨天时才把游标挪到 last, 于是"昨天签了、今天还没开灯"会算出 0 天
            // —— 可这恰恰是每天早上没到店时打开排行榜的常态, 等于天天把用户的连续天数清零。
            // 更早(< 昨天)则连续确实中断, 连续天数如实记 0; 累计天数 totalDays 另行展示, 行不会空。
            if (!last.before(yesterday)) {
                java.util.Set<String> dayStrs = new java.util.HashSet<>();
                for (java.util.Date d : sorted) dayStrs.add(DateUtil.formatDate(d));
                java.util.Date cursor = last;
                while (dayStrs.contains(DateUtil.formatDate(cursor))) {
                    streak++;
                    cursor = DateUtil.offsetDay(cursor, -1);
                }
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
     * 累计学习天数榜: 和连续签到榜<b>同源同聚合</b>, 只是换个排序口径。
     *
     * <p>复用 {@link #queryStreakRank()} 而不是重写一遍查询, 好处是两个标签页的数字永远一致 ——
     * 否则同一份签到数据在两处算出不同的累计天数, 用户一眼就看出不对。</p>
     */
    @Override
    public List<Map<String, Object>> queryDaysRank() {
        List<Map<String, Object>> list = queryStreakRank();
        list.sort((a, b) -> {
            int tb = b.get("totalDays") == null ? 0 : (int) b.get("totalDays");
            int ta = a.get("totalDays") == null ? 0 : (int) a.get("totalDays");
            if (tb != ta) {
                return Integer.compare(tb, ta);
            }
            // 累计天数相同时, 连续天数高的排前面(更自律)
            int sb = b.get("streak") == null ? 0 : (int) b.get("streak");
            int sa = a.get("streak") == null ? 0 : (int) a.get("streak");
            return Integer.compare(sb, sa);
        });
        return list;
    }

    /**
     * 今日早到排行榜: 按当天首次开灯时间升序(谁先到谁靠前)
     *
     * <p><b>两处此前的缺陷已修</b>:</p>
     * <ol>
     *   <li>页面写的是「今日 6 点后」，但原查询只按日期过滤 -> 凌晨开灯也能拿第一。
     *       现在真正加上 {@code check_in_time >= 今天 earlyStartHour 点} 的下限。</li>
     *   <li>原代码用 {@code seen.add()} 声称"每人只算最早一次"，但查询没有排序，
     *       实际取到的是数据库碰巧返回的第一行。现在显式 {@code order by check_in_time asc}，
     *       取到的才真是当天第一次。</li>
     * </ol>
     */
    @Override
    public List<Map<String, Object>> queryEarlyRank() {
        Date today = DateUtil.beginOfDay(new Date());
        Date tomorrow = DateUtil.offsetDay(today, 1);
        // 起算时刻: 默认今天 06:00(可配 studyroom.rank.early-start-hour)
        Date startAt = DateUtil.offsetHour(today, Math.max(0, earlyStartHour));
        LambdaQueryWrapper<DayCardConsumption> lqw = Wrappers.lambdaQuery();
        lqw.ge(DayCardConsumption::getConsumptionDate, today)
            .lt(DayCardConsumption::getConsumptionDate, tomorrow)
            .ge(DayCardConsumption::getCheckInTime, startAt)
            .isNotNull(DayCardConsumption::getCheckInTime)
            // 关键: 不排序的话 seen.add() 取到的是"碰巧第一行", 不是最早一次
            .orderByAsc(DayCardConsumption::getCheckInTime);
        List<DayCardConsumption> all = baseMapper.selectList(lqw);
        java.util.List<Map<String, Object>> result = new java.util.ArrayList<>();
        java.util.Set<Long> seen = new java.util.HashSet<>();
        for (DayCardConsumption c : all) {
            if (c.getUserId() == null || c.getCheckInTime() == null) continue;
            if (!seen.add(c.getUserId())) continue; // 已按时间升序, 取到的就是当天最早一次
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("userId", c.getUserId());
            item.put("checkInTime", c.getCheckInTime());
            item.put("nickname", getNickname(c.getUserId()));
            item.put("avatarUrl", getAvatar(c.getUserId()));
            result.add(item);
        }
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
     * 后台「释放座位」专用: 强制解除某张卡的锁座并清空锁座字段。
     *
     * <p>与用户端「请假停卡 / 取消锁座」的区别: <b>不消耗请假次数</b>(后台强制处置)。
     * 但长期卡(月卡/季卡)仍要冻结剩余天数 —— 与「取消锁座」同一口径,
     * 否则清掉锁座字段后下次预约会按卡面天数重新起算, 等于白送一个新周期。</p>
     *
     * @param cardUserId 卡用户ID(sr_card_user.id)
     * @return 结果描述; 该卡本来就没锁座时返回 null
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String releaseCardLockByAdmin(Long cardUserId) {
        if (cardUserId == null) {
            return null;
        }
        CardUserVo cu = cardUserService.queryById(cardUserId);
        if (cu == null || cu.getLockSeatId() == null) {
            return null;
        }
        String cardName = null;
        try {
            com.ruoyi.studyroom.domain.vo.CardVo cv = cardService.queryById(cu.getCardId());
            cardName = cv == null ? null : cv.getCardTitle();
        } catch (Exception ignore) {
        }
        int paused = -1;
        if (isLongTermCard(cu)) {
            paused = freezeRemaining(cu);
        }
        cardUserService.clearLockSeat(cu.getId());
        String msg = "已解除卡锁座「" + (cardName == null ? "卡" : cardName) + "」(座位 " + cu.getLockSeatId() + ")";
        if (paused >= 0) {
            msg += "，剩余 " + paused + " 天已冻结, 可在「我的卡券」选日期续用";
        }
        log.info("[后台释放] 强制解除卡锁座: cardUserId={} 卡={} -> {}", cardUserId, cardName, msg);
        return msg;
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
