package com.ruoyi.studyroom.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 积分账户 sr_points
 */
@Data
@TableName("sr_points")
public class Points implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer points;

    private Integer totalEarned;

    private Integer totalSpent;

    private Date createTime;

    private Date updateTime;
}
