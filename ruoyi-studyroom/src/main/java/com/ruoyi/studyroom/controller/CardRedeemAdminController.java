package com.ruoyi.studyroom.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.studyroom.domain.bo.CardRedeemBo;
import com.ruoyi.studyroom.domain.vo.CardRedeemVo;
import com.ruoyi.studyroom.domain.vo.CardVo;
import com.ruoyi.studyroom.service.ICardRedeemService;
import com.ruoyi.studyroom.service.ICardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

/**
 * 双人卡好友兑换码(后台)
 *
 * @author ruoyi
 */
@Validated
@Api(value = "双人卡兑换码", tags = {"双人卡兑换码"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/studyroom/cardRedeem")
public class CardRedeemAdminController extends BaseController {

    private final ICardRedeemService cardRedeemService;
    private final ICardService cardService;

    @ApiOperation("兑换码列表")
    @SaCheckPermission("studyroom:cardRedeem:list")
    @GetMapping("/list")
    public TableDataInfo<CardRedeemVo> list(CardRedeemBo bo, PageQuery pageQuery) {
        return cardRedeemService.queryPageList(bo, pageQuery);
    }

    @ApiOperation("兑换码详情")
    @SaCheckPermission("studyroom:cardRedeem:query")
    @GetMapping("/{id}")
    public R<CardRedeemVo> getInfo(@PathVariable("id") Long id) {
        return R.ok(cardRedeemService.queryById(id));
    }

    @ApiOperation("手工生成/补发兑换码")
    @SaCheckPermission("studyroom:cardRedeem:generate")
    @PostMapping("/generate")
    public R<CardRedeemVo> generate(@RequestBody CardRedeemBo bo) {
        if (bo.getCardId() == null) {
            throw new ServiceException("请选择卡券");
        }
        if (bo.getOwnerUserId() == null) {
            throw new ServiceException("请选择购买者(会员)");
        }
        CardVo card = cardService.queryById(bo.getCardId());
        if (card == null) {
            throw new ServiceException("卡券不存在");
        }
        bo.setId(null);
        bo.setCardTitle(card.getCardTitle());
        bo.setPartySize(card.getPartySize());
        if (bo.getTerm() == null && card.getTerm() != null) {
            bo.setTerm(card.getTerm().intValue());
        }
        if (bo.getStatus() == null) {
            bo.setStatus(0);
        }
        cardRedeemService.insertByBo(bo);
        return R.ok(cardRedeemService.queryById(bo.getId()));
    }

    @ApiOperation("批量生成通用兑换码(任意卡券, 不绑定购买者)")
    @SaCheckPermission("studyroom:cardRedeem:generate")
    @PostMapping("/batchGenerate")
    public R<List<CardRedeemVo>> batchGenerate(@RequestBody CardRedeemBo bo) {
        if (bo.getCardId() == null) {
            throw new ServiceException("请选择卡券");
        }
        int count = bo.getCount() == null ? 1 : bo.getCount();
        if (count < 1 || count > 200) {
            throw new ServiceException("单次生成数量需在 1~200 之间");
        }
        CardVo card = cardService.queryById(bo.getCardId());
        if (card == null) {
            throw new ServiceException("卡券不存在");
        }
        if (card.getStatus() != null && card.getStatus() == 0) {
            throw new ServiceException("该卡已停用，不能生成兑换码: " + card.getCardTitle());
        }
        int expireDays = bo.getExpireDays() == null || bo.getExpireDays() <= 0 ? 365 : bo.getExpireDays();
        List<CardRedeemVo> out = new java.util.ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            CardRedeemBo one = new CardRedeemBo();
            one.setCardId(card.getId());
            one.setCardTitle(card.getCardTitle());
            one.setPartySize(card.getPartySize());
            // 兑换后卡的有效期: 不填则用卡面天数(日卡1天/周卡7天/月卡31天...)
            one.setTerm(bo.getTerm() != null ? bo.getTerm()
                : (card.getTerm() == null ? null : card.getTerm().intValue()));
            // 通用兑换码不绑定购买者: owner_user_id 非空, 用 0 表示"后台生成"
            one.setOwnerUserId(0L);
            one.setOwnerNickname("后台生成");
            one.setStatus(0);
            one.setExpireTime(bo.getExpireTime() != null ? bo.getExpireTime()
                : cn.hutool.core.date.DateUtil.offsetDay(new java.util.Date(), expireDays));
            one.setRemark(com.ruoyi.common.utils.StringUtils.isBlank(bo.getRemark())
                ? "后台批量生成" : bo.getRemark());
            cardRedeemService.insertByBo(one);
            out.add(cardRedeemService.queryById(one.getId()));
        }
        return R.ok(out);
    }

    @ApiOperation("作废兑换码")
    @SaCheckPermission("studyroom:cardRedeem:edit")
    @PostMapping("/invalidate/{id}")
    public R<Void> invalidate(@PathVariable("id") Long id, @RequestParam(required = false) String reason) {
        return toAjax(cardRedeemService.invalidate(id, reason));
    }

    @ApiOperation("删除兑换码")
    @SaCheckPermission("studyroom:cardRedeem:remove")
    @DeleteMapping("/{ids}")
    public R<Void> remove(@PathVariable Long[] ids) {
        return toAjax(cardRedeemService.deleteWithValidByIds(Arrays.asList(ids), true));
    }
}
