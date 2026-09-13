package com.ruoyi.studyroom.domain.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

/**
 * 后台撤销权限请求
 *
 * @author studyroom
 */
@Data
@ApiModel("后台撤销权限请求")
public class AdminRevokeBo {

    /**
     * 锁座记录ID(与userId二选一, 优先按此)
     */
    @ApiModelProperty(value = "锁座记录ID(与userId二选一)")
    private Long lockId;

    /**
     * 会员ID(按用户撤销全部有效权限)
     */
    @ApiModelProperty(value = "会员ID(按用户撤销全部有效权限)")
    private Long userId;

    /**
     * 自习室ID(按用户撤销时可选, 缺省全部自习室)
     */
    @ApiModelProperty(value = "自习室ID")
    private Long roomId;

    /**
     * 撤销原因(记录用)
     */
    @ApiModelProperty(value = "撤销原因")
    private String reason;
}
