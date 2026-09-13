package com.ruoyi.studyroom.wx.api;

import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.helper.LoginHelper;
import com.ruoyi.studyroom.domain.vo.CardRedeemVo;
import com.ruoyi.studyroom.service.ICardRedeemService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 双人卡好友兑换(小程序端)
 *
 * 流程: 购买者付全款 → 系统给购买者发卡 + 生成兑换码 →
 *       购买者把兑换码发给同行好友 → 好友用自己的微信号在此兑换, 同样得到一张卡。
 *
 * @author ruoyi
 */
@RestController
@Api(value = "双人卡兑换", tags = "双人卡兑换")
@RequiredArgsConstructor
@RequestMapping("/api/wx/card/redeem")
public class CardRedeemController {

    private final ICardRedeemService cardRedeemService;

    @ApiOperation("兑换双人卡(好友用自己账号兑换)")
    @PostMapping
    public R<Map<String, Object>> redeem(@RequestBody Map<String, String> body) {
        Long userId = LoginHelper.getUserId();
        String code = body == null ? null : body.get("code");
        return R.ok(cardRedeemService.redeem(code, userId));
    }

    @ApiOperation("我的兑换码(购买者分享给同伴)")
    @GetMapping("/mine")
    public R<List<CardRedeemVo>> mine() {
        return R.ok(cardRedeemService.queryMine(LoginHelper.getUserId()));
    }
}
