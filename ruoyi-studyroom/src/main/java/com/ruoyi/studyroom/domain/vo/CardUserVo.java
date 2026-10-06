package com.ruoyi.studyroom.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import com.ruoyi.studyroom.domain.Card;
import com.ruoyi.studyroom.domain.User;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;



/**
 * 优惠卡-会员关联视图对象 sr_card_user
 *
 * @author P_Peaceful
 * @date 2022-05-08
 */
@Data
@ApiModel("优惠卡-会员关联视图对象")
@ExcelIgnoreUnannotated
public class CardUserVo {

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
     * 卡片ID
     */
    @ExcelProperty(value = "卡片ID")
    @ApiModelProperty("卡片ID")
    private Long cardId;

    private CardVo card;

    @ExcelProperty(value = "过期时间")
    @ApiModelProperty("过期时间")
    private Date expiryTime;

    /**
     * 锁定座位ID(周卡/月卡预约后自动锁定)
     */
    @ApiModelProperty("锁定座位ID")
    private Long lockSeatId;

    /**
     * 锁座开始时间
     */
    @ApiModelProperty("锁座开始时间")
    private Date lockStartTime;

    /**
     * 锁座结束时间
     */
    @ApiModelProperty("锁座结束时间")
    private Date lockEndTime;

    /**
     * 已请假次数
     */
    @ApiModelProperty("已请假次数")
    private Integer leaveCount;

    /**
     * 请假额度(该卡总共可请假次数, 默认3, 后台可追加)
     */
    @ApiModelProperty("请假额度(该卡总共可请假次数)")
    private Integer leaveQuota;

    /**
     * 剩余可请假次数(已用次数相对额度的余量, 仅展示用)
     */
    @ApiModelProperty("剩余可请假次数")
    private Integer leaveRemain;

    /**
     * 长期卡剩余可用天数(未激活=term, 停卡后为冻结剩余)
     */
    @ApiModelProperty("剩余可用天数")
    private Integer remainingDays;

    /**
     * 是否已使用(存在日卡消费记录)
     */
    @ApiModelProperty("是否已使用")
    private Boolean consumed;

    /**
     * 开卡日期(购买时间)
     */
    @ApiModelProperty("开卡日期")
    private Date createTime;

}
