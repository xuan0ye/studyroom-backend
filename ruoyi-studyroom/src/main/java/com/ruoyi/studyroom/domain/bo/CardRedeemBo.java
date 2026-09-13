package com.ruoyi.studyroom.domain.bo;

import com.ruoyi.common.core.validate.EditGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;
import java.util.Date;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 双人卡好友兑换码业务对象 sr_card_redeem
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("双人卡好友兑换码业务对象")
public class CardRedeemBo extends BaseEntity {

    @ApiModelProperty(value = "主键", required = true)
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    @ApiModelProperty(value = "兑换码")
    private String code;

    @ApiModelProperty(value = "卡券ID")
    private Long cardId;

    @ApiModelProperty(value = "卡券名称")
    private String cardTitle;

    @ApiModelProperty(value = "兑换后有效期(天)")
    private Integer term;

    @ApiModelProperty(value = "套餐人数")
    private Integer partySize;

    @ApiModelProperty(value = "购买者用户ID")
    private Long ownerUserId;

    @ApiModelProperty(value = "购买者昵称")
    private String ownerNickname;

    @ApiModelProperty(value = "购卡订单号")
    private String orderId;

    @ApiModelProperty(value = "状态 0未使用 1已使用 2已作废")
    private Integer status;

    @ApiModelProperty(value = "兑换人用户ID")
    private Long usedByUserId;

    @ApiModelProperty(value = "兑换人昵称")
    private String usedByNickname;

    @ApiModelProperty(value = "兑换时间")
    private Date usedTime;

    @ApiModelProperty(value = "兑换码有效期")
    private Date expireTime;

    @ApiModelProperty(value = "备注")
    private String remark;

}
