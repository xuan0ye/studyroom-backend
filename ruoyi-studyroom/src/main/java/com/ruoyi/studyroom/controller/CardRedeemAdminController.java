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
