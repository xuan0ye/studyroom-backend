package com.ruoyi.studyroom.domain.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 后台特殊卡修改请求
 *
 * @author studyroom
 */
@Data
@ApiModel("后台特殊卡修改请求")
public class AdminCardUpdateBo {

    /**
     * 用户卡记录ID(sr_card_user.id)
     */
    @ApiModelProperty(value = "用户卡记录ID(sr_card_user.id)", required = true)
    @NotNull(message = "用户卡记录ID不能为空")
    private Long cardUserId;

    /**
     * 新的到期时间(不传则不修改)
     */
    @ApiModelProperty(value = "新的到期时间")
    private Date expiryTime;

    /**
     * 已请假次数(不传则不修改)
     */
    @ApiModelProperty(value = "已请假次数")
    private Integer leaveCount;

    /**
     * 请假额度: 该卡总共可请假的次数(不传则不修改)
     */
    @ApiModelProperty(value = "请假额度(该卡总共可请假的次数)")
    private Integer leaveQuota;

    /**
     * 追加请假额度: 在原有额度上累加(不传则不修改)
     */
    @ApiModelProperty(value = "追加请假额度(在原有额度上累加)")
    private Integer leaveQuotaAdd;

    /**
     * 修改说明
     */
    @ApiModelProperty(value = "修改说明")
    private String reason;
}
