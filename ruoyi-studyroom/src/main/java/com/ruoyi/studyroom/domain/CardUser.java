package com.ruoyi.studyroom.domain;

import com.baomidou.mybatisplus.annotation.*;
import com.ruoyi.studyroom.domain.vo.CardVo;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Date;
import java.math.BigDecimal;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 优惠卡-会员关联对象 sr_card_user
 *
 * @author P_Peaceful
 * @date 2022-05-08
 */
@Data
@TableName("sr_card_user")
public class CardUser extends BaseEntity {

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
     * 卡片ID
     */
    private Long cardId;

    /**
     * 过期时间
     */
    private Date expiryTime;

    /**
     * 锁定座位ID(周卡/月卡预约后自动锁定)
     */
    private Long lockSeatId;

    /**
     * 锁座开始时间
     */
    private Date lockStartTime;

    /**
     * 锁座结束时间
     */
    private Date lockEndTime;

    /**
     * 已请假次数
     */
    private Integer leaveCount;

    /**
     * 请假额度(该卡总共可请假的次数, 默认3, 后台可追加)
     */
    private Integer leaveQuota;

    /**
     * 长期卡剩余可用天数(停卡冻结/续启结算, 默认=卡term)
     */
    private Integer remainingDays;

    @TableField(exist = false)
    private Card card;
}
