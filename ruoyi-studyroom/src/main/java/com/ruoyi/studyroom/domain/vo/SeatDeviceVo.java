package com.ruoyi.studyroom.domain.vo;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 座位设备 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SeatDeviceVo extends BaseEntity {

    private Long deviceId;
    private Long seatId;
    private String deviceType;
    private String deviceName;
    private String protocol;
    private String deviceUid;
    private String mqttTopic;
    private Integer status;
    private Integer autoControl;
    private Long roomId;
    /** 关联座位编号 */
    private String seatNum;
    /** 关联座位区域 */
    private String areaName;
}
