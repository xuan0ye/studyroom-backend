package com.ruoyi.studyroom.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Date;
import java.math.BigDecimal;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 日卡消费记录对象 sr_day_card_consumption
 *
 * @author P_Peaceful
 * @date 2022-05-10
 */
@Data
@TableName("sr_day_card_consumption")
public class DayCardConsumption extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;
    /**
     * 会员ID
     */
    private Long userId;
    /**
     * 会员卡关联ID
     */
    private Long cardUserId;
    /**
     * 座位ID
     */
    private Long seatId;
    /**
     * 消费日期
     */
    private Date consumptionDate;
    /**
     * 状态: 0-待使用, 1-已使用, 2-已失效
     */
    private Integer status;

    /**
     * 签到开灯时间(每天首次开灯即签到)
     */
    private Date checkInTime;

}
