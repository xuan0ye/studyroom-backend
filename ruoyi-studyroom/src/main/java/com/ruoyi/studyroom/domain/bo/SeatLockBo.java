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
 * 座位预占/锁定业务对象 sr_seat_lock
 *
 * @author P_Peaceful
 * @date 2022-05-10
 */

@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("座位预占/锁定业务对象")
public class SeatLockBo extends BaseEntity {

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
     * 座位ID
     */
    @ApiModelProperty(value = "座位ID", required = true)
    @NotNull(message = "座位ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long seatId;

    /**
     * 自习室ID
     */
    @ApiModelProperty(value = "自习室ID", required = true)
    @NotNull(message = "自习室ID不能为空", groups = { AddGroup.class, EditGroup.class })
    private Long roomId;

    /**
     * 锁定类型: 0-定时锁定, 1-手动锁定, 2-自动续期
     */
    @ApiModelProperty(value = "锁定类型")
    private Integer lockType;

    /**
     * 锁定开始日期
     */
    @ApiModelProperty(value = "锁定开始日期")
    private Date startDate;

    /**
     * 锁定结束日期
     */
    @ApiModelProperty(value = "锁定结束日期")
    private Date endDate;

    /**
     * 状态: 0-待激活, 1-激活, 2-已过期, 3-已取消
     */
    @ApiModelProperty(value = "状态")
    private Integer status;

    /**
     * 自动续期: 0-否, 1-是
     */
    @ApiModelProperty(value = "自动续期")
    private Integer autoRenew;

}
