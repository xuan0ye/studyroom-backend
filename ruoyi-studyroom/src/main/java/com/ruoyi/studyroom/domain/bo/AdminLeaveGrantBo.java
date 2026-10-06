package com.ruoyi.studyroom.domain.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 后台发放请假次数请求
 *
 * <p>给指定用户卡追加请假额度(每张长期卡默认 3 次, 用完可在后台追加)。</p>
 *
 * @author studyroom
 */
@Data
@ApiModel("后台发放请假次数请求")
public class AdminLeaveGrantBo {

    /**
     * 用户卡记录ID(sr_card_user.id)
     */
    @ApiModelProperty(value = "用户卡记录ID(sr_card_user.id)", required = true)
    @NotNull(message = "用户卡记录ID不能为空")
    private Long cardUserId;

    /**
     * 本次追加的请假次数(正整数, 在原有额度上累加)
     */
    @ApiModelProperty(value = "本次追加的请假次数(正整数)", required = true)
    @NotNull(message = "追加次数不能为空")
    private Integer count;

    /**
     * 发放说明(记录用)
     */
    @ApiModelProperty(value = "发放说明")
    private String reason;
}
