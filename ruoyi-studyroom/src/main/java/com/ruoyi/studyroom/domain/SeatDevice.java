package com.ruoyi.studyroom.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * @ProjectName: study-room-vue
 * @Author: WenZhengcheng
 * @Date: 2022-5-17
 * @Desc: 座位-IoT设备映射表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sr_seat_device")
public class SeatDevice extends BaseEntity {

    @TableId
    private Long deviceId;

    /** 关联座位ID */
    private Long seatId;

    /** 设备类型: LIGHT-灯光, PLUG-插座, AC-空调 */
    private String deviceType;

    /** 设备名称 */
    private String deviceName;

    /** 设备通信协议: MQTT, MIIO, MIOT_API */
    private String protocol;

    /** 设备唯一标识(MQTT topic/miio did) */
    private String deviceUid;

    /** MQTT消息主题 */
    private String mqttTopic;

    /** 开指令 payload */
    private String onPayload;

    /** 关指令 payload */
    private String offPayload;

    /** 设备状态: 0-离线, 1-在线, 2-已开, 3-已关 */
    private Integer status;

    /** 是否启用自动控制: 0-否, 1-是 */
    private Integer autoControl;

    /** 关联房间ID */
    private Long roomId;
}
