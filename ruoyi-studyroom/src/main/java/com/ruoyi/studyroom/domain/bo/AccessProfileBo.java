package com.ruoyi.studyroom.domain.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 门禁用户档案查询参数
 *
 * @author studyroom
 */
@Data
@ApiModel("门禁用户档案查询参数")
public class AccessProfileBo {

    /**
     * 用户ID
     */
    @ApiModelProperty("用户ID")
    private Long userId;

    /**
     * 自习室ID
     */
    @ApiModelProperty("自习室ID")
    private Long roomId;

    /**
     * 座位ID
     */
    @ApiModelProperty("座位ID")
    private Long seatId;

    /**
     * 门禁名(模糊)
     */
    @ApiModelProperty("门禁名")
    private String userName;

    /**
     * 同步状态: 0-未同步, 1-已同步
     */
    @ApiModelProperty("同步状态: 0-未同步 1-已同步")
    private Integer syncStatus;

    /**
     * 撤销状态: 0-未撤销, 1-已撤销, 2-等待重试
     */
    @ApiModelProperty("撤销状态: 0-未撤销 1-已撤销 2-等待重试")
    private Integer revokeStatus;
}
