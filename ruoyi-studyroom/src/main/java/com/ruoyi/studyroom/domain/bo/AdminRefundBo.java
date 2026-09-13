package com.ruoyi.studyroom.domain.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import java.math.BigDecimal;

/**
 * 后台退款请求
 *
 * @author studyroom
 */
@Data
@ApiModel("后台退款请求")
public class AdminRefundBo {

    /**
     * 订单ID(sr_order.id)
     */
    @ApiModelProperty(value = "订单ID(sr_order.id)")
    private Long orderId;

    /**
     * 订单号(order_id 字符串, 与orderId二选一)
     */
    @ApiModelProperty(value = "订单号(order_id, 与orderId二选一)")
    private String orderNo;

    /**
     * 退款金额(缺省=订单全额)
     */
    @ApiModelProperty(value = "退款金额(缺省=订单全额)")
    private BigDecimal amount;

    /**
     * 退款说明
     */
    @ApiModelProperty(value = "退款说明")
    private String reason;
}
