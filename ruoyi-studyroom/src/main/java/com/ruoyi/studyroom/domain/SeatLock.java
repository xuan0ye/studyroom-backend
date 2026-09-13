package com.ruoyi.studyroom.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;
import java.util.Date;
import java.math.BigDecimal;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 座位预占/锁定对象 sr_seat_lock
 *
 * @author P_Peaceful
 * @date 2022-05-10
 */
@Data
@TableName("sr_seat_lock")
public class SeatLock extends BaseEntity {

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
     * 座位ID
     */
    private Long seatId;
    /**
     * 自习室ID
     */
    private Long roomId;
    /**
     * 锁定类型: 0-定时锁定, 1-手动锁定, 2-自动续期
     */
    private Integer lockType;
    /**
     * 锁定开始日期
     */
    private Date startDate;
    /**
     * 锁定结束日期
     */
    private Date endDate;
    /**
     * 状态: 0-待激活, 1-激活, 2-已过期, 3-已取消
     */
    private Integer status;
    /**
     * 自动续期: 0-否, 1-是
     */
    private Integer autoRenew;

}
