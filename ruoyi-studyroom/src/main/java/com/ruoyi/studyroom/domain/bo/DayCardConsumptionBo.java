package com.ruoyi.studyroom.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import javax.validation.constraints.*;

import java.util.Date;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 日卡消费记录业务对象 sr_day_card_consumption
 *
 * @author P_Peaceful
 * @date 2022-05-10
 */

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("日卡消费记录业务对象")
public class DayCardConsumptionBo extends BaseEntity {

    /**
     * 主键ID
     */
    @ApiModelProperty(value = "主键ID", required = true)
    @NotNull(message = "主键ID不能为空", groups = { EditGroup.class })
    private Long id;

    /**
     * 会员ID
     */
    @ApiModelProperty(value = "会员ID", required = true)
    @NotNull(message = "会员ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long userId;

    /**
     * 会员卡关联ID
     */
    @ApiModelProperty(value = "会员卡关联ID", required = true)
    @NotNull(message = "会员卡关联ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long cardUserId;

    /**
     * 座位ID
     */
    @ApiModelProperty(value = "座位ID")
    private Long seatId;

    /**
     * 消费日期
     */
    @ApiModelProperty(value = "消费日期")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(
        using = com.ruoyi.studyroom.util.FlexibleDateDeserializer.class)
    private Date consumptionDate;

    /**
     * 状态: 0-待使用, 1-已使用, 2-已失效
     */
    @ApiModelProperty(value = "状态")
    private Integer status;

    /**
     * 签到开灯时间(每天首次开灯即签到)
     */
    @ApiModelProperty(value = "签到开灯时间")
    private Date checkInTime;

}
