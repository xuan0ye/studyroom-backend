package com.ruoyi.studyroom.domain.bo;

import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 座位设备 BO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SeatDeviceBo extends BaseEntity {

    private Long deviceId;
    private Long seatId;
    private String deviceType;
    private String deviceName;
    private String protocol;
    private String deviceUid;
    private String mqttTopic;
    private String onPayload;
    private String offPayload;
    private Integer status;
    private Integer autoControl;
    private Long roomId;
}
