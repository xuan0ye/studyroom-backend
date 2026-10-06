package com.ruoyi.studyroom.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import cn.dev33.satoken.annotation.SaMode;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.R;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.studyroom.domain.bo.AdminOccupyBo;
import com.ruoyi.studyroom.domain.bo.AdminRevokeBo;
import com.ruoyi.studyroom.domain.bo.AdminRefundBo;
import com.ruoyi.studyroom.domain.bo.AdminCardGrantBo;
import com.ruoyi.studyroom.domain.bo.AdminCardUpdateBo;
import com.ruoyi.studyroom.domain.bo.AdminLeaveGrantBo;
import com.ruoyi.studyroom.service.IStudyroomAdminService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 自习室后台运营操作
 *
 * <p>提供管理员的代占座/锁座、撤销权限、退款、特殊卡发放/修改等运营能力,
 * 用于客服/店长在后台直接替用户操作(不依赖小程序端)。</p>
 *
 * @author studyroom
 */
@Validated
@Api(value = "自习室运营操作控制器", tags = {"自习室运营操作"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/studyroom/admin")
public class StudyroomAdminController extends BaseController {

    private final IStudyroomAdminService studyroomAdminService;

    /**
     * 代占座/锁座: 管理员直接为用户锁定座位(写占座记录+锁座记录)
     */
    @ApiOperation("代占座/锁座")
    @SaCheckPermission("studyroom:admin:occupy")
    @Log(title = "代占座/锁座", businessType = BusinessType.INSERT)
    @PostMapping("/occupy")
    public R<Void> occupy(@Validated @RequestBody AdminOccupyBo bo) {
        return toAjax(studyroomAdminService.occupy(bo));
    }

    /**
     * 撤销权限: 按锁座记录ID或用户ID撤销门禁授权
     */
    @ApiOperation("撤销权限")
    @SaCheckPermission("studyroom:admin:revoke")
    @Log(title = "撤销权限", businessType = BusinessType.UPDATE)
    @PostMapping("/revoke")
    public R<Void> revoke(@Validated @RequestBody AdminRevokeBo bo) {
        return toAjax(studyroomAdminService.revoke(bo));
    }

    /**
     * 退款: 按订单号退款, 金额退回用户余额
     */
    @ApiOperation("退款")
    @SaCheckPermission("studyroom:admin:refund")
    @Log(title = "退款", businessType = BusinessType.UPDATE)
    @PostMapping("/refund")
    public R<Void> refund(@Validated @RequestBody AdminRefundBo bo) {
        return toAjax(studyroomAdminService.refund(bo));
    }

    /**
     * 特殊卡发放: 直接给用户发放一张卡(不经过支付)
     */
    @ApiOperation("特殊卡发放")
    @SaCheckPermission("studyroom:admin:card:grant")
    @Log(title = "特殊卡发放", businessType = BusinessType.INSERT)
    @PostMapping("/card/grant")
    public R<Void> grantCard(@Validated @RequestBody AdminCardGrantBo bo) {
        return toAjax(studyroomAdminService.grantCard(bo));
    }

    /**
     * 删除会员卡券: 释放锁座/占座, 作废预约记录, 撤销门禁后删除卡券
     */
    @ApiOperation("删除会员卡券(含清理锁座/占座)")
    @SaCheckPermission("studyroom:admin:card:revoke")
    @Log(title = "删除会员卡券", businessType = BusinessType.DELETE)
    @PostMapping("/card/revoke")
    public R<String> revokeCard(@RequestBody java.util.Map<String, Object> body) {
        Object idObj = body == null ? null : body.get("cardUserId");
        if (idObj == null) {
            return R.fail("请传入 cardUserId");
        }
        Long cardUserId = Long.valueOf(String.valueOf(idObj).trim());
        Object reasonObj = body.get("reason");
        String reason = reasonObj == null ? null : String.valueOf(reasonObj);
        return R.ok(studyroomAdminService.revokeCard(cardUserId, reason));
    }

    /**
     * 特殊卡修改: 调整用户卡的到期时间/已请假次数/请假额度
     */
    @ApiOperation("特殊卡修改")
    @SaCheckPermission("studyroom:admin:card:update")
    @Log(title = "特殊卡修改", businessType = BusinessType.UPDATE)
    @PutMapping("/card")
    public R<Void> updateCard(@Validated @RequestBody AdminCardUpdateBo bo) {
        return toAjax(studyroomAdminService.updateCard(bo));
    }

    /**
     * 发放请假次数: 在用户卡原有请假额度上追加(每张长期卡默认 3 次)
     * 权限上同时接受「运营-发卡」与「卡券管理-编辑」, 避免门店账号没配运营权限时用不了
     */
    @ApiOperation("发放请假次数")
    @SaCheckPermission(value = {"studyroom:admin:card:update", "studyroom:cardUser:edit"}, mode = SaMode.OR)
    @Log(title = "发放请假次数", businessType = BusinessType.UPDATE)
    @PostMapping("/card/leave/grant")
    public R<String> grantLeave(@Validated @RequestBody AdminLeaveGrantBo bo) {
        try {
            return R.ok(studyroomAdminService.grantLeave(bo));
        } catch (com.ruoyi.common.exception.ServiceException e) {
            return R.fail(e.getMessage());
        }
    }
}
