package com.ruoyi.studyroom.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.util.Date;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 双人卡好友兑换码对象 sr_card_redeem
 *
 * @author ruoyi
 */
@Data
@TableName("sr_card_redeem")
public class CardRedeem extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 兑换码
     */
    private String code;

    /**
     * 要发放的卡券ID
     */
    private Long cardId;

    /**
     * 卡券名称(冗余)
     */
    private String cardTitle;

    /**
     * 兑换后有效期(天)
     */
    private Integer term;

    /**
     * 套餐人数
     */
    private Integer partySize;

    /**
     * 购买者(生成码的人)
     */
    private Long ownerUserId;

    /**
     * 购买者昵称(冗余)
     */
    private String ownerNickname;

    /**
     * 购卡订单号
     */
    private String orderId;

    /**
     * 状态: 0未使用 1已使用 2已作废
     */
    private Integer status;

    /**
     * 兑换人
     */
    private Long usedByUserId;

    /**
     * 兑换人昵称(冗余)
     */
    private String usedByNickname;

    /**
     * 兑换时间
     */
    private Date usedTime;

    /**
     * 兑换码有效期
     */
    private Date expireTime;

    /**
     * 备注
     */
    private String remark;

}
