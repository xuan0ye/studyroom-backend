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
 * 日卡消费记录视图对象 sr_day_card_consumption
 *
 * @author P_Peaceful
 * @date 2022-05-10
 */
@Data
@ApiModel("日卡消费记录视图对象")
@ExcelIgnoreUnannotated
public class DayCardConsumptionVo {

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
     * 会员卡关联ID
     */
    @ExcelProperty(value = "会员卡关联ID")
    @ApiModelProperty("会员卡关联ID")
    private Long cardUserId;

    /**
     * 座位ID
     */
    @ExcelProperty(value = "座位ID")
    @ApiModelProperty("座位ID")
    private Long seatId;

    /**
     * 消费日期
     */
    @ExcelProperty(value = "消费日期")
    @ApiModelProperty("消费日期")
    private Date consumptionDate;

    /**
     * 状态: 0-待使用, 1-已使用, 2-已失效
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "consumption_status")
    @ApiModelProperty("状态")
    private Integer status;

    /**
     * 签到开灯时间(每天首次开灯即签到)
     */
    @ApiModelProperty("签到开灯时间")
    private Date checkInTime;

    /**
     * 座位号
     */
    @ApiModelProperty("座位号")
    private Integer seatNum;

    /**
     * 门店名称
     */
    @ApiModelProperty("门店名称")
    private String roomName;

    /**
     * 卡名称
     */
    @ApiModelProperty("卡名称")
    private String cardTitle;

    /**
     * 用户昵称(后台预约记录用)
     */
    @ApiModelProperty("用户昵称")
    private String nickname;

    /**
     * 用户手机号(后台预约记录用)
     */
    @ApiModelProperty("用户手机号")
    private String phone;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    private Date createTime;

}
