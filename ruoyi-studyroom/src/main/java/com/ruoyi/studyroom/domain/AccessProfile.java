package com.ruoyi.studyroom.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sr_access_profile")
public class AccessProfile extends BaseEntity {

    @TableId
    private Long id;

    private Long userId;
    private Long roomId;
    private Long seatId;
    private String orderId;
    private String deviceSn;
    private Long jobnumber;
    private String userName;
    private String facePhotoUrl;
    private String facePhotoBase64;
    private Long validity;
    private Integer syncStatus;
    private String syncErrmsg;
    private Integer recordPic;
    private Integer revokeStatus;
    private Integer revokeRetryCount;
    private java.util.Date nextRevokeRetryTime;

    /**
     * 用户昵称(展示用, 联表 sr_user)
     */
    @TableField(exist = false)
    private String nickname;

    /**
     * 用户手机号(展示用, 联表 sr_user)
     */
    @TableField(exist = false)
    private String userPhone;

    /**
     * 座位号(展示用, 联表 sr_seat)
     */
    @TableField(exist = false)
    private Integer seatNum;

    /**
     * 座位区域名(展示用, 联表 sr_seat)
     */
    @TableField(exist = false)
    private String areaName;
}
