package com.ruoyi.studyroom.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sr_access_device")
public class AccessDevice extends BaseEntity {

    @TableId
    private Long id;

    private String sn;
    private String registerNo;
    private String deviceName;
    private Integer deviceType;
    private Long roomId;
    private String deviceIp;
    private String wifiSsid;
    private Integer status;
    private java.util.Date lastOnlineTime;
    private String remark;
}
