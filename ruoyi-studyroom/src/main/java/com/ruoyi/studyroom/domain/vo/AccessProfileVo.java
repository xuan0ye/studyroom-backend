package com.ruoyi.studyroom.domain.vo;

import lombok.Data;

@Data
public class AccessProfileVo {
    private String orderId;
    private Long roomId;
    private Long seatId;
    private String deviceSn;
    private Long jobnumber;
    private Integer syncStatus;
    private String syncErrmsg;
    private Long validity;
    private String facePhotoUrl;
}
