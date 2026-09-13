package com.ruoyi.studyroom.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 积分流水 VO
 */
@Data
public class PointsLogVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("变动积分(正加负减)")
    private Integer changePoints;

    @ApiModelProperty("变动后积分")
    private Integer balancePoints;

    @ApiModelProperty("类型 1签到 2审核发放 3兑换消耗 4抵扣消耗 5退回")
    private Integer type;

    @ApiModelProperty("备注")
    private String remark;

    @ApiModelProperty("时间")
    private Date createTime;
}
