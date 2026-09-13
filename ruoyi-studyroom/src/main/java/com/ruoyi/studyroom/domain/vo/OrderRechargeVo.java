package com.ruoyi.studyroom.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;



/**
 * 充值订单视图对象 sr_order_recharge
 *
 * @author P_Peaceful
 * @date 2022-05-08
 */
@Data
@ApiModel("充值订单视图对象")
@ExcelIgnoreUnannotated
public class OrderRechargeVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    @ApiModelProperty("主键")
    private Long id;

    /**
     * 订单编号
     */
    @ExcelProperty(value = "订单编号")
    @ApiModelProperty("订单编号")
    private String orderId;

    /**
     * 会员ID
     */
    @ExcelProperty(value = "会员ID")
    @ApiModelProperty("会员ID")
    private Long userId;

    /**
     * 商品描述
     */
    @ExcelProperty(value = "商品描述")
    @ApiModelProperty("商品描述")
    private String description;

    /**
     * 支付金额
     */
    @ExcelProperty(value = "支付金额")
    @ApiModelProperty("支付金额")
    private BigDecimal amountTotal;

    /**
     * 充值额度
     */
    @ExcelProperty(value = "充值额度")
    @ApiModelProperty("充值额度")
    private Integer rechargeTotal;

    /**
     * 付款状态
     */
    @ExcelProperty(value = "付款状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "pay_status")
    @ApiModelProperty("付款状态")
    private Integer payStatus;

    /**
     * 用户昵称(联表 sr_user)
     */
    @ApiModelProperty("用户昵称")
    private String userNickname;

    /**
     * 用户手机号(联表 sr_user)
     */
    @ApiModelProperty("用户手机号")
    private String userPhone;

    /**
     * 充值时间
     */
    @ApiModelProperty("充值时间")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

}
