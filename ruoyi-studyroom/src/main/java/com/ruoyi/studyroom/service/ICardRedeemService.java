package com.ruoyi.studyroom.service;

import com.ruoyi.studyroom.domain.vo.CardRedeemVo;
import com.ruoyi.studyroom.domain.bo.CardRedeemBo;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 双人卡好友兑换码Service接口
 *
 * 业务: 双人套餐由一人付全款(如 799/人 x 2), 系统给购买者发一张卡,
 * 同时生成一个兑换码; 购买者把兑换码发给同伴, 同伴用自己的微信号兑换后也会得到一张同样的卡。
 *
 * @author ruoyi
 */
public interface ICardRedeemService {

    CardRedeemVo queryById(Long id);

    TableDataInfo<CardRedeemVo> queryPageList(CardRedeemBo bo, PageQuery pageQuery);

    List<CardRedeemVo> queryList(CardRedeemBo bo);

    Boolean insertByBo(CardRedeemBo bo);

    Boolean updateByBo(CardRedeemBo bo);

    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 购卡成功后为购买者生成好友兑换码(幂等: 同一订单只生成一个)
     *
     * @param cardId      卡券ID
     * @param cardTitle   卡券名称
     * @param term        兑换后有效期(天)
     * @param partySize   套餐人数(>=2 才生成)
     * @param ownerUserId 购买者
     * @param orderId     购卡订单号
     * @return 兑换码记录(已存在时返回原记录)
     */
    CardRedeemVo generateForOrder(Long cardId, String cardTitle, Integer term, Integer partySize, Long ownerUserId, String orderId);

    /**
     * 我(购买者)生成的兑换码
     */
    List<CardRedeemVo> queryMine(Long ownerUserId);

    /**
     * 好友兑换: 校验并发放卡券
     *
     * @return {success, msg, cardTitle, expiryTime, code}
     */
    Map<String, Object> redeem(String code, Long userId);

    /**
     * 后台作废兑换码
     */
    Boolean invalidate(Long id, String reason);
}
