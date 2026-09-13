package com.ruoyi.studyroom.domain.vo;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 积分账户 VO
 */
@Data
public class PointsVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @ApiModelProperty("当前积分")
    private Integer points;

    @ApiModelProperty("累计获得")
    private Integer totalEarned;

    @ApiModelProperty("累计消耗")
    private Integer totalSpent;

    @ApiModelProperty("更新时间")
    private Date updateTime;
}
