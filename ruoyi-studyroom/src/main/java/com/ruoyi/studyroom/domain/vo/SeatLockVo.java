package com.ruoyi.studyroom.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;



/**
 * 座位预占/锁定视图对象 sr_seat_lock
 *
 * @author P_Peaceful
 * @date 2022-05-10
 */
@Data
@ApiModel("座位预占/锁定视图对象")
@ExcelIgnoreUnannotated
public class SeatLockVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    @ApiModelProperty("主键ID")
    private Long id;

    /**
     * 会员ID
     */
    @ExcelProperty(value = "会员ID")
    @ApiModelProperty("会员ID")
    private Long userId;

    /**
     * 座位ID
     */
    @ExcelProperty(value = "座位ID")
    @ApiModelProperty("座位ID")
    private Long seatId;

    /**
     * 自习室ID
     */
    @ExcelProperty(value = "自习室ID")
    @ApiModelProperty("自习室ID")
    private Long roomId;

    /**
     * 锁定类型: 0-定时锁定, 1-手动锁定, 2-自动续期
     */
    @ExcelProperty(value = "锁定类型", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "seat_lock_type")
    @ApiModelProperty("锁定类型")
    private Integer lockType;

    /**
     * 锁定开始日期
     */
    @ExcelProperty(value = "锁定开始日期")
    @ApiModelProperty("锁定开始日期")
    private Date startDate;

    /**
     * 锁定结束日期
     */
    @ExcelProperty(value = "锁定结束日期")
    @ApiModelProperty("锁定结束日期")
    private Date endDate;

    /**
     * 状态: 0-待激活, 1-激活, 2-已过期, 3-已取消
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "seat_lock_status")
    @ApiModelProperty("状态")
    private Integer status;

    /**
     * 自动续期: 0-否, 1-是
     */
    @ExcelProperty(value = "自动续期", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "sys_yes_no")
    @ApiModelProperty("自动续期")
    private Integer autoRenew;

    /**
     * 座位号
     */
    @ApiModelProperty("座位号")
    private Integer seatNum;

}
