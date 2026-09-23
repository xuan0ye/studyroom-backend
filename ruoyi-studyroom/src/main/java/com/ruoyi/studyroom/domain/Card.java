package com.ruoyi.studyroom.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Date;
import java.math.BigDecimal;

import java.math.BigDecimal;
import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 优惠卡对象 sr_card
 *
 * @author P_Peaceful
 * @date 2022-05-08
 */
@Data
@TableName("sr_card")
public class Card extends BaseEntity {

    private static final long serialVersionUID=1L;

    /**
     * 主键ID
     */
    @TableId(value = "id")
    private Long id;
    /**
     * 优惠卡
     */
    private String cardTitle;
    /**
     * 价格
     */
    private BigDecimal price;
    /**
     * 可用区域
     */
    private Integer usable;
    /**
     * 次数(天)
     */
    private Long frequency;
    /**
     * 有效期(天)
     */
    private Long term;
    /**
     * 状态
     */
    private Integer status;

    /**
     * 区域限制: 0=不限区域 1=仅沉浸区(16-19号) 2=禁止沉浸区(16-19号)
     */
    private Integer cardType;

    /**
     * 每日最长可用小时(预留)
     */
    private Integer maxDailyHours;

    /**
     * 套餐人数: 1=单人 2=双人(购买后额外生成 1 个好友兑换码)
     */
    private Integer partySize;

    /**
     * 固定使用起始日(与 validEnd 同时存在时为"固定区间卡": 只能在区间内使用, 且每天单独预约、不连锁锁座)
     * 例如中秋国庆畅享券: 2026-09-25 ~ 2026-10-07
     */
    private Date validStart;

    /**
     * 固定使用截止日(含当天)
     */
    private Date validEnd;

}
