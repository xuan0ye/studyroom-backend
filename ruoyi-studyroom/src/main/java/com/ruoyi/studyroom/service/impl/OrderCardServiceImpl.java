package com.ruoyi.studyroom.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.constant.PayConstants;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ruoyi.studyroom.domain.bo.CardUserBo;
import com.ruoyi.studyroom.domain.vo.CardVo;
import com.ruoyi.studyroom.service.ICardService;
import com.ruoyi.studyroom.service.ICardUserService;
import com.ruoyi.studyroom.service.ICardRedeemService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import com.ruoyi.studyroom.domain.bo.OrderCardBo;
import com.ruoyi.studyroom.domain.vo.OrderCardVo;
import com.ruoyi.studyroom.domain.OrderCard;
import com.ruoyi.studyroom.domain.User;
import com.ruoyi.studyroom.domain.Card;
import com.ruoyi.studyroom.mapper.OrderCardMapper;
import com.ruoyi.studyroom.mapper.UserMapper;
import com.ruoyi.studyroom.mapper.CardMapper;
import com.ruoyi.studyroom.service.IOrderCardService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 购卡管理Service业务层处理
 *
 * @author P_Peaceful
 * @date 2022-05-10
 */
@Slf4j
@RequiredArgsConstructor
@Service
public class OrderCardServiceImpl implements IOrderCardService {

    private final OrderCardMapper baseMapper;
    private final ICardUserService cardUserService;
    private final ICardService cardService;
    private final ICardRedeemService cardRedeemService;
    private final UserMapper userMapper;
    private final CardMapper cardMapper;

    /**
     * 补全关联展示字段(用户昵称/手机号, 卡标题, 开卡时间, 卡到期时间)
     */
    private void fillDisplayFields(OrderCardVo vo) {
        if (vo == null) {
            return;
        }
        if (vo.getUserId() != null) {
            User user = userMapper.selectById(vo.getUserId());
            if (user != null) {
                vo.setUserNickname(user.getNickname());
                vo.setUserPhone(user.getPhone());
            }
        }
        if (vo.getCardId() != null) {
            Card card = cardMapper.selectById(vo.getCardId());
            if (card != null) {
                vo.setCardTitle(card.getCardTitle());
                vo.setCardTerm(card.getTerm());
            }
        }
        if (vo.getCreateTime() != null && vo.getCardTerm() != null) {
            vo.setExpiryTime(DateUtil.offsetDay(vo.getCreateTime(), vo.getCardTerm().intValue()));
        }
    }

    /**
     * 查询购卡管理
     *
     * @param id 购卡管理主键
     * @return 购卡管理
     */
    @Override
    public OrderCardVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询购卡管理列表
     *
     * @param bo 购卡管理
     * @return 购卡管理
     */
    @Override
    public TableDataInfo<OrderCardVo> queryPageList(OrderCardBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<OrderCard> lqw = buildQueryWrapper(bo);
        Page<OrderCardVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        if (result.getRecords() != null) {
            result.getRecords().forEach(this::fillDisplayFields);
        }
        return TableDataInfo.build(result);
    }

    /**
     * 查询购卡管理列表
     *
     * @param bo 购卡管理
     * @return 购卡管理
     */
    @Override
    public List<OrderCardVo> queryList(OrderCardBo bo) {
        LambdaQueryWrapper<OrderCard> lqw = buildQueryWrapper(bo);
        List<OrderCardVo> list = baseMapper.selectVoList(lqw);
        if (list != null) {
            list.forEach(this::fillDisplayFields);
        }
        return list;
    }

    private LambdaQueryWrapper<OrderCard> buildQueryWrapper(OrderCardBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<OrderCard> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getUserId() != null, OrderCard::getUserId, bo.getUserId());
        lqw.eq(bo.getCardId() != null, OrderCard::getCardId, bo.getCardId());
        lqw.eq(StringUtils.isNotBlank(bo.getOrderId()), OrderCard::getOrderId, bo.getOrderId());
        lqw.eq(StringUtils.isNotBlank(bo.getDescription()), OrderCard::getDescription, bo.getDescription());
        lqw.eq(bo.getAmountTotal() != null, OrderCard::getAmountTotal, bo.getAmountTotal());
        lqw.eq(bo.getPayStatus() != null, OrderCard::getPayStatus, bo.getPayStatus());
        return lqw;
    }

    /**
     * 新增购卡管理
     *
     * @param bo 购卡管理
     * @return 结果
     */
    @Override
    public Boolean insertByBo(OrderCardBo bo) {
        OrderCard add = BeanUtil.toBean(bo, OrderCard.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改购卡管理
     *
     * @param bo 购卡管理
     * @return 结果
     */
    @Override
    public Boolean updateByBo(OrderCardBo bo) {
        OrderCard update = BeanUtil.toBean(bo, OrderCard.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Boolean updateByBoAndCard(OrderCardBo bo) {
        if (bo.getOrderId() == null || bo.getCardId() == null || bo.getUserId() == null) {
            return false;
        }
        // 幂等闸门: 仅当订单从未支付(UNPAID=0)时置为已支付, 影响行数=1 才发卡;
        // 重复回调/轮询/并发时已处理过则直接返回成功, 杜绝重复发卡
        int rows = baseMapper.update(null, Wrappers.<OrderCard>lambdaUpdate()
            .eq(OrderCard::getOrderId, bo.getOrderId())
            .eq(OrderCard::getPayStatus, PayConstants.UNPAID)
            .set(OrderCard::getPayStatus, PayConstants.PAID));
        if (rows <= 0) {
            // 可能已支付(重复通知)或订单不存在
            OrderCardVo exist = queryByOrderId(bo.getOrderId());
            return exist != null && PayConstants.PAID.equals(exist.getPayStatus());
        }
        // 首次置为已支付 → 发卡
        CardVo cardVo = cardService.queryById(bo.getCardId());
        if (cardVo == null || cardVo.getTerm() == null) {
            throw new com.ruoyi.common.exception.ServiceException("卡信息异常，发卡失败");
        }
        int term = cardVo.getTerm().intValue();
        String now = DateUtil.now();
        DateTime time = DateUtil.parse(now);
        CardUserBo cardUserBo = new CardUserBo();
        cardUserBo.setUserId(bo.getUserId());
        cardUserBo.setCardId(bo.getCardId());
        cardUserBo.setExpiryTime(DateUtil.offsetDay(time, term));
        boolean issued = cardUserService.insertByBo(cardUserBo);

        // 双人套餐(party_size>=2): 购买者额外获得一个好友兑换码, 同伴用自己的微信号兑换后同样发卡
        try {
            Integer partySize = cardVo.getPartySize();
            if (issued && partySize != null && partySize >= 2) {
                cardRedeemService.generateForOrder(cardVo.getId(), cardVo.getCardTitle(), term, partySize,
                    bo.getUserId(), bo.getOrderId());
            }
        } catch (Exception e) {
            // 兑换码生成失败不影响已付款的购卡结果, 可在后台「运营操作-兑换码」手工补发
            log.error("[双人卡] 生成好友兑换码失败, orderId={}, cardId={}", bo.getOrderId(), bo.getCardId(), e);
        }
        return issued;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(OrderCard entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除购卡管理
     *
     * @param ids 需要删除的购卡管理主键
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
     * 根据订单ID查询
     *
     * @param orderId
     * @return
     */
    @Override
    public OrderCardVo queryByOrderId(String orderId) {
        LambdaQueryWrapper<OrderCard> lqw = Wrappers.lambdaQuery();
        lqw.eq(OrderCard::getOrderId, orderId);
        return baseMapper.selectVoOne(lqw);
    }
}
