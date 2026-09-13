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
     * 新的离开次数(不传则不修改)
     */
    @ApiModelProperty(value = "新的离开次数")
    private Integer leaveCount;

    /**
     * 修改说明
     */
    @ApiModelProperty(value = "修改说明")
    private String reason;
}
