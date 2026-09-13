package com.ruoyi.studyroom.service.impl;

import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.redis.RedisUtils;
import com.ruoyi.studyroom.domain.DayCardConsumption;
import com.ruoyi.studyroom.domain.Points;
import com.ruoyi.studyroom.domain.PointsLog;
import com.ruoyi.studyroom.domain.RecoverRequest;
import com.ruoyi.studyroom.domain.vo.CardUserVo;
import com.ruoyi.studyroom.domain.vo.PointsLogVo;
import com.ruoyi.studyroom.domain.vo.PointsVo;
import com.ruoyi.studyroom.mapper.DayCardConsumptionMapper;
import com.ruoyi.studyroom.mapper.PointsLogMapper;
import com.ruoyi.studyroom.mapper.PointsMapper;
import com.ruoyi.studyroom.mapper.RecoverRequestMapper;
import com.ruoyi.studyroom.service.ICardService;
import com.ruoyi.studyroom.service.ICardUserService;
import com.ruoyi.studyroom.service.IPointsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 积分服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PointsServiceImpl implements IPointsService {

    private static final String CHECKIN_POINTS_KEY = "studyroom:points:checkin:";
    // 积分商城: 3000积分换1张日卡
    private static final int DAY_CARD_COST = 3000;
    private static final String EXCHANGE_LOCK_KEY = "studyroom:points:exchange:";

    private final PointsMapper pointsMapper;
    private final PointsLogMapper pointsLogMapper;
    private final DayCardConsumptionMapper dayCardConsumptionMapper;
    private final RecoverRequestMapper recoverRequestMapper;
    private final ICardUserService cardUserService;
    private final ICardService cardService;

    @Override
    public PointsVo getByUserId(Long userId) {
        Points p = getAccount(userId);
        PointsVo vo = new PointsVo();
        vo.setPoints(p.getPoints());
        vo.setTotalEarned(p.getTotalEarned());
        vo.setTotalSpent(p.getTotalSpent());
        vo.setUpdateTime(p.getUpdateTime());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int addByCheckIn(Long userId) {
        // 每天仅首次签到加分
        String today = DateUtil.formatDate(new Date());
        String key = CHECKIN_POINTS_KEY + userId + ":" + today;
        if (RedisUtils.hasKey(key)) {
            return 0;
        }
        // 计算连续签到天数(含今天): 从今天往前数连续有签到记录的日期数
        int streak = calcStreakIncludingToday(userId);
        // 积分规则: 连续第1天100, 每天+10, 第10天190, 第11天起200封顶
        int points;
        if (streak <= 1) {
            points = 100;
        } else if (streak >= 11) {
            points = 200;
        } else {
            points = 90 + streak * 10; // streak=2 ->110, 3->120, ... 10->190
        }
        change(userId, points, 1, "每日签到奖励(连续" + streak + "天)");
        long ttlSeconds = DateUtil.betweenMs(
            DateUtil.beginOfDay(new Date()), DateUtil.offsetDay(DateUtil.beginOfDay(new Date()), 1)) / 1000;
        RedisUtils.setCacheObject(key, "1", Math.max(ttlSeconds, 1), TimeUnit.SECONDS);
        return points;
    }

    /**
     * 计算用户连续签到天数(含今天, 若今天已签到则从今天起算; 否则从昨天起算)
     */
    private int calcStreakIncludingToday(Long userId) {
        LambdaQueryWrapper<DayCardConsumption> lqw = Wrappers.lambdaQuery();
        lqw.eq(DayCardConsumption::getUserId, userId)
            .isNotNull(DayCardConsumption::getCheckInTime)
            .orderByAsc(DayCardConsumption::getConsumptionDate);
        List<DayCardConsumption> all = dayCardConsumptionMapper.selectList(lqw);
        Set<String> dayStrs = new HashSet<>();
        for (DayCardConsumption c : all) {
            if (c.getConsumptionDate() != null) {
                dayStrs.add(DateUtil.formatDate(c.getConsumptionDate()));
            }
        }
        if (dayStrs.isEmpty()) {
            return 1; // 今天首次签到, 连续1天
        }
        Date today = DateUtil.beginOfDay(new Date());
        Date cursor;
        // 若今天已签到, 从今天起算; 否则从昨天起算(今天的签到正在发生)
        if (dayStrs.contains(DateUtil.formatDate(today))) {
            cursor = today;
        } else {
            cursor = DateUtil.offsetDay(today, -1);
        }
        int streak = 0;
        while (dayStrs.contains(DateUtil.formatDate(cursor))) {
            streak++;
            cursor = DateUtil.offsetDay(cursor, -1);
        }
        return Math.max(streak, 1);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grantByAdmin(Long userId, int points, String remark, String operator) {
        if (points <= 0) {
            throw new ServiceException("发放积分必须大于0");
        }
        change(userId, points, 2, StringUtils.isBlank(remark) ? "客服审核发放" : remark);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void spend(Long userId, int points, int type, String remark) {
        if (points <= 0) {
            throw new ServiceException("消耗积分必须大于0");
        }
        // 同用户并发扣分锁(防 read-modify-write 丢失更新导致双重消费)
        String lockKey = "points:spend:" + userId;
        if (RedisUtils.hasKey(lockKey)) {
            throw new ServiceException("积分操作处理中，请稍候");
        }
        RedisUtils.setCacheObject(lockKey, "1", 10, TimeUnit.SECONDS);
        try {
            Points p = getAccount(userId);
            if (p.getPoints() == null || p.getPoints() < points) {
                throw new ServiceException("积分不足");
            }
            change(userId, -points, type, remark);
        } finally {
            RedisUtils.deleteObject(lockKey);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long exchangeDayCard(Long userId) {
        // 防并发重复兑换
        String lockKey = EXCHANGE_LOCK_KEY + userId;
        if (RedisUtils.hasKey(lockKey)) {
            throw new ServiceException("兑换处理中，请稍候");
        }
        RedisUtils.setCacheObject(lockKey, "1", 10, TimeUnit.SECONDS);
        try {
            Points p = getAccount(userId);
            if (p.getPoints() == null || p.getPoints() < DAY_CARD_COST) {
                throw new ServiceException("积分不足，需要 " + DAY_CARD_COST + " 积分兑换日卡");
            }
            // 找一张日卡(term=1)
            com.ruoyi.studyroom.domain.vo.CardVo dayCard = findDayCard();
            if (dayCard == null) {
                throw new ServiceException("暂无可兑换的日卡，请联系客服");
            }
            // 消耗积分
            change(userId, -DAY_CARD_COST, 3, "积分商城兑换日卡");
            // 发卡(有效期从今天起 term 天)
            Date now = new Date();
            com.ruoyi.studyroom.domain.bo.CardUserBo cu = new com.ruoyi.studyroom.domain.bo.CardUserBo();
            cu.setUserId(userId);
            cu.setCardId(dayCard.getId());
            cu.setExpiryTime(DateUtil.offsetDay(now, dayCard.getTerm() == null ? 1 : dayCard.getTerm().intValue()));
            boolean ok = cardUserService.insertByBo(cu);
            if (!ok) {
                throw new ServiceException("发卡失败，请联系客服");
            }
            return cu.getId();
        } finally {
            RedisUtils.deleteObject(lockKey);
        }
    }

    private com.ruoyi.studyroom.domain.vo.CardVo findDayCard() {
        com.ruoyi.studyroom.domain.bo.CardBo bo = new com.ruoyi.studyroom.domain.bo.CardBo();
        bo.setStatus(1);
        List<com.ruoyi.studyroom.domain.vo.CardVo> cards = cardService.queryList(bo);
        for (com.ruoyi.studyroom.domain.vo.CardVo c : cards) {
            if (c.getTerm() != null && c.getTerm().intValue() == 1) {
                return c;
            }
        }
        return cards.isEmpty() ? null : cards.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long submitRecover(Long userId, String missedDate, String reason, String contact) {
        if (StringUtils.isBlank(missedDate)) {
            throw new ServiceException("请选择漏签日期");
        }
        if (StringUtils.isBlank(reason)) {
            throw new ServiceException("请填写补签说明(好评内容)");
        }
        RecoverRequest rr = new RecoverRequest();
        rr.setUserId(userId);
        rr.setMissedDate(DateUtil.parseDate(missedDate));
        rr.setReason(reason.trim());
        rr.setContact(contact);
        rr.setStatus(0);
        rr.setCreateTime(new Date());
        recoverRequestMapper.insert(rr);
        return rr.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handleRecover(Long id, boolean pass, String remark, String handler) {
        RecoverRequest rr = recoverRequestMapper.selectById(id);
        if (rr == null) {
            throw new ServiceException("补签申请不存在");
        }
        if (rr.getStatus() != null && rr.getStatus() != 0) {
            throw new ServiceException("该申请已处理");
        }
        rr.setStatus(pass ? 1 : 2);
        rr.setRemark(remark);
        rr.setHandleTime(new Date());
        rr.setHandler(handler);
        recoverRequestMapper.updateById(rr);
        if (pass) {
            // 补签通过: 补发漏签当天的基础积分(100), 并补一条签到记录恢复连续性
            grantByAdmin(rr.getUserId(), 100, "补签通过(" + DateUtil.formatDate(rr.getMissedDate()) + ")",
                handler == null ? "admin" : handler);
            // 补签到记录: 在漏签日插入一条签到消费记录(check_in_time 非空)
            insertMissedCheckIn(rr.getUserId(), rr.getMissedDate());
        }
    }

    private void insertMissedCheckIn(Long userId, Date missedDate) {
        try {
            // 查找该用户漏签当天是否有消费记录, 有则补 check_in_time; 无则新建
            LambdaQueryWrapper<DayCardConsumption> lqw = Wrappers.lambdaQuery();
            lqw.eq(DayCardConsumption::getUserId, userId)
                .eq(DayCardConsumption::getConsumptionDate, DateUtil.beginOfDay(missedDate))
                .last("limit 1");
            DayCardConsumption exist = dayCardConsumptionMapper.selectOne(lqw);
            if (exist != null) {
                if (exist.getCheckInTime() == null) {
                    DayCardConsumption upd = new DayCardConsumption();
                    upd.setId(exist.getId());
                    upd.setCheckInTime(missedDate);
                    dayCardConsumptionMapper.updateById(upd);
                }
            } else {
                DayCardConsumption nc = new DayCardConsumption();
                nc.setUserId(userId);
                nc.setConsumptionDate(DateUtil.beginOfDay(missedDate));
                nc.setStatus(0);
                nc.setCheckInTime(missedDate);
                nc.setCreateTime(new Date());
                dayCardConsumptionMapper.insert(nc);
            }
        } catch (Exception e) {
            log.warn("补签记录写入失败 userId={} date={}", userId, missedDate, e);
        }
    }

    private void change(Long userId, int delta, int type, String remark) {
        Points p = getAccount(userId);
        int cur = p.getPoints() == null ? 0 : p.getPoints();
        int newBalance = cur + delta;
        if (newBalance < 0) {
            newBalance = 0;
        }
        p.setPoints(newBalance);
        if (delta > 0) {
            p.setTotalEarned((p.getTotalEarned() == null ? 0 : p.getTotalEarned()) + delta);
        } else {
            p.setTotalSpent((p.getTotalSpent() == null ? 0 : p.getTotalSpent()) + (-delta));
        }
        p.setUpdateTime(new Date());
        pointsMapper.updateById(p);
        // 流水
        PointsLog log = new PointsLog();
        log.setUserId(userId);
        log.setChangePoints(delta);
        log.setBalancePoints(newBalance);
        log.setType(type);
        log.setRemark(remark);
        log.setCreateTime(new Date());
        pointsLogMapper.insert(log);
    }

    private Points getAccount(Long userId) {
        LambdaQueryWrapper<Points> lqw = Wrappers.lambdaQuery();
        lqw.eq(Points::getUserId, userId);
        Points p = pointsMapper.selectOne(lqw);
        if (p == null) {
            p = new Points();
            p.setUserId(userId);
            p.setPoints(0);
            p.setTotalEarned(0);
            p.setTotalSpent(0);
            p.setCreateTime(new Date());
            p.setUpdateTime(new Date());
            pointsMapper.insert(p);
        }
        return p;
    }

    @Override
    public List<PointsLogVo> listLogs(Long userId, int limit) {
        LambdaQueryWrapper<PointsLog> lqw = Wrappers.lambdaQuery();
        lqw.eq(PointsLog::getUserId, userId)
            .orderByDesc(PointsLog::getCreateTime)
            .last("limit " + Math.max(1, Math.min(limit, 100)));
        return pointsLogMapper.selectVoList(lqw);
    }
}
