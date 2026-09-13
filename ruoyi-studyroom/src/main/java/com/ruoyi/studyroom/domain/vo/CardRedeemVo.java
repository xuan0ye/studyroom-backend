package com.ruoyi.studyroom.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 双人卡好友兑换码视图对象 sr_card_redeem
 *
 * @author ruoyi
 */
@Data
@ApiModel("双人卡好友兑换码视图对象")
@ExcelIgnoreUnannotated
public class CardRedeemVo {

    private static final long serialVersionUID = 1L;

    @ExcelProperty(value = "主键")
    @ApiModelProperty("主键")
    private Long id;

    @ExcelProperty(value = "兑换码")
    @ApiModelProperty("兑换码")
    private String code;

    @ExcelProperty(value = "卡券ID")
    @ApiModelProperty("卡券ID")
    private Long cardId;

    @ExcelProperty(value = "卡券名称")
    @ApiModelProperty("卡券名称")
    private String cardTitle;

    @ExcelProperty(value = "有效期(天)")
    @ApiModelProperty("兑换后有效期(天)")
    private Integer term;

    @ExcelProperty(value = "套餐人数")
    @ApiModelProperty("套餐人数")
    private Integer partySize;

    @ExcelProperty(value = "购买者ID")
    @ApiModelProperty("购买者ID")
    private Long ownerUserId;

    @ExcelProperty(value = "购买者")
    @ApiModelProperty("购买者昵称")
    private String ownerNickname;

    @ExcelProperty(value = "购卡订单号")
    @ApiModelProperty("购卡订单号")
    private String orderId;

    @ExcelProperty(value = "状态")
    @ApiModelProperty("状态 0未使用 1已使用 2已作废")
    private Integer status;

    @ExcelProperty(value = "兑换人ID")
    @ApiModelProperty("兑换人ID")
    private Long usedByUserId;

    @ExcelProperty(value = "兑换人")
    @ApiModelProperty("兑换人昵称")
    private String usedByNickname;

    @ExcelProperty(value = "兑换时间")
    @ApiModelProperty("兑换时间")
    private Date usedTime;

    @ExcelProperty(value = "兑换码有效期")
    @ApiModelProperty("兑换码有效期")
    private Date expireTime;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("创建时间")
    private Date createTime;

}
