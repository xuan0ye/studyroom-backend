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
import com.ruoyi.studyroom.domain.bo.CardRedeemBo;
import com.ruoyi.studyroom.domain.bo.CardUserBo;
import com.ruoyi.studyroom.domain.vo.CardRedeemVo;
import com.ruoyi.studyroom.domain.vo.CardUserVo;
import com.ruoyi.studyroom.domain.vo.UserVo;
import com.ruoyi.studyroom.domain.CardRedeem;
import com.ruoyi.studyroom.mapper.CardRedeemMapper;
import com.ruoyi.studyroom.service.ICardRedeemService;
import com.ruoyi.studyroom.service.ICardUserService;
import com.ruoyi.studyroom.service.IUserService;

import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 双人卡好友兑换码Service业务层处理
 *
 * @author ruoyi
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class CardRedeemServiceImpl implements ICardRedeemService {

    /** 兑换码字符集: 去掉易混的 0/O/1/I/L */
    private static final String CODE_CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";
    private static final int CODE_LEN = 8;
    /** 兑换码有效期(天), 自购买之日起 */
    private static final int CODE_VALID_DAYS = 30;

    private final CardRedeemMapper baseMapper;
    private final ICardUserService cardUserService;
    private final IUserService userService;

    @Override
    public CardRedeemVo queryById(Long id) {
        return baseMapper.selectVoById(id);
    }

    @Override
    public TableDataInfo<CardRedeemVo> queryPageList(CardRedeemBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<CardRedeem> lqw = buildQueryWrapper(bo);
        Page<CardRedeemVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    @Override
    public List<CardRedeemVo> queryList(CardRedeemBo bo) {
        return baseMapper.selectVoList(buildQueryWrapper(bo));
    }

    private LambdaQueryWrapper<CardRedeem> buildQueryWrapper(CardRedeemBo bo) {
        LambdaQueryWrapper<CardRedeem> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getId() != null, CardRedeem::getId, bo.getId());
        lqw.eq(bo.getCardId() != null, CardRedeem::getCardId, bo.getCardId());
        lqw.eq(bo.getStatus() != null, CardRedeem::getStatus, bo.getStatus());
        lqw.eq(bo.getOwnerUserId() != null, CardRedeem::getOwnerUserId, bo.getOwnerUserId());
        lqw.eq(StringUtils.isNotBlank(bo.getCode()), CardRedeem::getCode, bo.getCode() == null ? null : bo.getCode().trim().toUpperCase());
        lqw.like(StringUtils.isNotBlank(bo.getOwnerNickname()), CardRedeem::getOwnerNickname, bo.getOwnerNickname());
        lqw.like(StringUtils.isNotBlank(bo.getUsedByNickname()), CardRedeem::getUsedByNickname, bo.getUsedByNickname());
        lqw.orderByDesc(CardRedeem::getCreateTime).orderByDesc(CardRedeem::getId);
        return lqw;
    }

    @Override
    public Boolean insertByBo(CardRedeemBo bo) {
        CardRedeem add = BeanUtil.toBean(bo, CardRedeem.class);
        if (StringUtils.isBlank(add.getCode())) {
            add.setCode(newCode());
        }
        if (add.getStatus() == null) {
            add.setStatus(0);
        }
        if (add.getExpireTime() == null) {
            add.setExpireTime(DateUtil.offsetDay(new Date(), CODE_VALID_DAYS));
        }
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    @Override
    public Boolean updateByBo(CardRedeemBo bo) {
        CardRedeem update = BeanUtil.toBean(bo, CardRedeem.class);
        return baseMapper.updateById(update) > 0;
    }

    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 购卡成功 → 生成好友兑换码(幂等: 同一订单只生成一个)
     */
    @Override
    public CardRedeemVo generateForOrder(Long cardId, String cardTitle, Integer term, Integer partySize,
                                         Long ownerUserId, String orderId) {
        if (cardId == null || ownerUserId == null) {
            return null;
        }
        if (partySize == null || partySize < 2) {
            return null;
        }
        // 幂等: 同一订单已生成过则直接返回
        if (StringUtils.isNotBlank(orderId)) {
            List<CardRedeemVo> exist = baseMapper.selectVoList(
                Wrappers.<CardRedeem>lambdaQuery().eq(CardRedeem::getOrderId, orderId).last("limit 1"));
            if (exist != null && !exist.isEmpty()) {
                return exist.get(0);
            }
        }
        CardRedeem entity = new CardRedeem();
        entity.setCode(newCode());
        entity.setCardId(cardId);
        entity.setCardTitle(cardTitle);
        entity.setTerm(term);
        entity.setPartySize(partySize);
        entity.setOwnerUserId(ownerUserId);
        entity.setOwnerNickname(nickname(ownerUserId));
        entity.setOrderId(orderId);
        entity.setStatus(0);
        entity.setExpireTime(DateUtil.offsetDay(new Date(), CODE_VALID_DAYS));
        entity.setRemark("双人套餐自动生成");
        baseMapper.insert(entity);
        log.info("[双人卡] 生成好友兑换码: code={}, 购买者={}, 订单={}, 卡={}({}天)",
            entity.getCode(), ownerUserId, orderId, cardTitle, term);
        return baseMapper.selectVoById(entity.getId());
    }

    @Override
    public List<CardRedeemVo> queryMine(Long ownerUserId) {
        if (ownerUserId == null) {
            return java.util.Collections.emptyList();
        }
        return baseMapper.selectVoList(Wrappers.<CardRedeem>lambdaQuery()
            .eq(CardRedeem::getOwnerUserId, ownerUserId)
            .orderByDesc(CardRedeem::getCreateTime));
    }

    /**
     * 好友兑换
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> redeem(String code, Long userId) {
        Map<String, Object> res = new HashMap<>();
        String c = code == null ? "" : code.trim().toUpperCase().replace(" ", "");
        if (c.isEmpty()) {
            return fail(res, "请输入兑换码");
        }
        List<CardRedeemVo> list = baseMapper.selectVoList(
            Wrappers.<CardRedeem>lambdaQuery().eq(CardRedeem::getCode, c).last("limit 1"));
        if (list == null || list.isEmpty()) {
            return fail(res, "兑换码不存在，请核对后重试");
        }
        CardRedeemVo vo = list.get(0);
        if (vo.getStatus() != null && vo.getStatus() == 2) {
            return fail(res, "该兑换码已作废");
        }
        if (vo.getStatus() != null && vo.getStatus() == 1) {
            return fail(res, "该兑换码已被使用" + (StringUtils.isNotBlank(vo.getUsedByNickname()) ? "（" + vo.getUsedByNickname() + "）" : ""));
        }
        if (vo.getExpireTime() != null && vo.getExpireTime().before(new Date())) {
            return fail(res, "兑换码已过期（有效期至 " + fmt(vo.getExpireTime()) + "），请联系购买人重新获取");
        }
        if (vo.getOwnerUserId() != null && vo.getOwnerUserId().equals(userId)) {
            return fail(res, "不能兑换自己购买的兑换码，请把它发给同行好友");
        }
        // 已持有同款未过期卡(未激活也算持有) → 不允许重复兑换(防刷)
        List<CardUserVo> mine = cardUserService.queryListByUserId(userId);
        if (mine != null) {
            Date now = new Date();
            for (CardUserVo cu : mine) {
                if (cu.getCardId() != null && cu.getCardId().equals(vo.getCardId())
                    && (cu.getExpiryTime() == null || cu.getExpiryTime().after(now))) {
                    return fail(res, "你已持有该卡券且未过期，无需重复兑换");
                }
            }
        }
        // 原子占用: 只有把 0->1 改成功的人才能发卡(防并发重复兑换)
        int rows = baseMapper.update(null, Wrappers.<CardRedeem>lambdaUpdate()
            .eq(CardRedeem::getId, vo.getId())
            .eq(CardRedeem::getStatus, 0)
            .set(CardRedeem::getStatus, 1)
            .set(CardRedeem::getUsedByUserId, userId)
            .set(CardRedeem::getUsedByNickname, nickname(userId))
            .set(CardRedeem::getUsedTime, new Date()));
        if (rows <= 0) {
            return fail(res, "该兑换码已被使用，请核对");
        }
        // 发卡: 不写有效期, 自首次预约使用起算 term 天
        int term = vo.getTerm() == null ? 100 : vo.getTerm();
        CardUserBo cu = new CardUserBo();
        cu.setUserId(userId);
        cu.setCardId(vo.getCardId());
        if (!cardUserService.insertByBo(cu)) {
            throw new ServiceException("发卡失败，请联系客服处理");
        }
        log.info("[双人卡] 兑换成功: code={}, 兑换人={}, 卡={}, 可用天数={}", c, userId, vo.getCardTitle(), term);
        res.put("success", true);
        res.put("msg", "兑换成功！「" + (vo.getCardTitle() == null ? "卡券" : vo.getCardTitle())
            + "」已发放到你的卡包，首次预约使用后 " + term + " 天内有效");
        res.put("cardTitle", vo.getCardTitle());
        res.put("code", c);
        return res;
    }

    @Override
    public Boolean invalidate(Long id, String reason) {
        int rows = baseMapper.update(null, Wrappers.<CardRedeem>lambdaUpdate()
            .eq(CardRedeem::getId, id)
            .eq(CardRedeem::getStatus, 0)
            .set(CardRedeem::getStatus, 2)
            .set(CardRedeem::getRemark, StringUtils.isBlank(reason) ? "后台作废" : reason));
        return rows > 0;
    }

    // ==================== 工具 ====================

    private Map<String, Object> fail(Map<String, Object> res, String msg) {
        res.put("success", false);
        res.put("msg", msg);
        return res;
    }

    private String fmt(Date d) {
        return d == null ? "-" : new SimpleDateFormat("yyyy-MM-dd").format(d);
    }

    private String nickname(Long userId) {
        try {
            UserVo u = userService.queryById(userId);
            return u == null ? null : u.getNickname();
        } catch (Exception e) {
            return null;
        }
    }

    /** 生成不重复的兑换码 */
    private String newCode() {
        SecureRandom rnd = new SecureRandom();
        for (int attempt = 0; attempt < 30; attempt++) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < CODE_LEN; i++) {
                sb.append(CODE_CHARS.charAt(rnd.nextInt(CODE_CHARS.length())));
            }
            String code = sb.toString();
            Long cnt = baseMapper.selectCount(Wrappers.<CardRedeem>lambdaQuery().eq(CardRedeem::getCode, code));
            if (cnt == null || cnt == 0) {
                return code;
            }
        }
        throw new ServiceException("兑换码生成失败，请稍后重试");
    }
}
