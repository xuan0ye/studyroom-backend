package com.ruoyi.studyroom.domain.vo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.util.Date;

/**
 * 当前座位占用(后台「释放座位」列表用)
 * <p>
 * 一行 = 一条 sr_seat_record(座位占用记录); 同时带出它关联的
 * 手动锁座 / 卡锁座、以及日卡消费记录, 便于后台一眼看出"这个座位是被什么占着"。
 *
 * @author P_Peaceful
 */
@Data
@ApiModel("当前座位占用")
public class SeatOccupationVo {

    @ApiModelProperty("占座记录ID(sr_seat_record.id)")
    private Long recordId;

    @ApiModelProperty("会员ID")
    private Long userId;

    @ApiModelProperty("会员昵称")
    private String nickname;

    @ApiModelProperty("会员手机号")
    private String phone;

    @ApiModelProperty("座位ID")
    private Long seatId;

    @ApiModelProperty("座位号")
    private Integer seatNum;

    @ApiModelProperty("自习室ID")
    private Long roomId;

    @ApiModelProperty("开始时间")
    private Date startTime;

    @ApiModelProperty("结束时间")
    private Date endTime;

    @ApiModelProperty("关联的手动锁座ID(sr_seat_lock.id)")
    private Long lockId;

    @ApiModelProperty("关联的持卡记录ID(sr_card_user.id, 卡锁座)")
    private Long cardUserId;

    @ApiModelProperty("卡ID")
    private Long cardId;

    @ApiModelProperty("卡名")
    private String cardTitle;

    @ApiModelProperty("关联的日卡消费记录ID(sr_day_card_consumption.id)")
    private Long consumptionId;

    @ApiModelProperty("占用来源: 代占座/手动锁座、卡锁座、日卡预约、其它(由后台按关联数据判定)")
    private String source;

    @ApiModelProperty("占用天数(按记录跨度算)")
    private Integer days;
}