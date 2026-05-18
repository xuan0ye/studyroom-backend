package com.ruoyi.studyroom.domain;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 设备控制请求
 */
@Data
@Accessors(chain = true)
public class DeviceControlRequest {
    private Long deviceId;
    private Long seatId;
    /** true-开, false-关 */
    private Boolean turnOn;
}
