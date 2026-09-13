package com.ruoyi.studyroom.domain.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 后台代占座/锁座请求
 *
 * @author studyroom
 */
@Data
@ApiModel("后台代占座/锁座请求")
public class AdminOccupyBo {

    /**
     * 会员ID
     */
    @ApiModelProperty(value = "会员ID", required = true)
    @NotNull(message = "会员ID不能为空")
    private Long userId;

    /**
     * 座位ID
     */
    @ApiModelProperty(value = "座位ID", required = true)
    @NotNull(message = "座位ID不能为空")
    private Long seatId;

    /**
     * 自习室ID
     */
    @ApiModelProperty(value = "自习室ID", required = true)
    @NotNull(message = "自习室ID不能为空")
    private Long roomId;

    /**
     * 占座开始时间(缺省=今天00:00)
     */
    @ApiModelProperty(value = "占座开始时间")
    private Date startTime;

    /**
     * 占座结束时间(缺省=开始时间+1天)
     */
    @ApiModelProperty(value = "占座结束时间")
    private Date endTime;

    /**
     * 是否同时创建长期锁座记录: 0-否, 1-是(缺省0)
     */
    @ApiModelProperty(value = "是否创建锁座记录 0-否 1-是")
    private Integer createLock;

    /**
     * 锁座开始日期(createLock=1时生效, 格式 yyyy-MM-dd)
     */
    @ApiModelProperty(value = "锁座开始日期")
    private String lockStartDate;

    /**
     * 锁座结束日期(createLock=1时生效, 格式 yyyy-MM-dd)
     */
    @ApiModelProperty(value = "锁座结束日期")
    private String lockEndDate;
}
