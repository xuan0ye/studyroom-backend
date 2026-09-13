package com.ruoyi.studyroom.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 积分流水 sr_points_log
 */
@Data
@TableName("sr_points_log")
public class PointsLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer changePoints;

    private Integer balancePoints;

    private Integer type;

    private String remark;

    private Date createTime;
}
