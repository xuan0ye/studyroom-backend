package com.ruoyi.studyroom.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

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

    /**
     * 当前是否允许远程开门(当天存在有效预约)。
     *
     * <p>前端"远程开门"按钮的显示依据。<b>不要再用 syncStatus==1 做判断</b>:
     * 该值表示"人脸命令已下发", 与"现在能不能开门"是两件事,
     * 且历史实现中无订阅者时也会置 1(假成功)。</p>
     */
    private Boolean canOpenDoor;

    /**
     * 本自习室门禁设备最后在线时间; null = 从未接入系统。
     * 供前端/后台展示"门禁是否真的在线"。
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date deviceLastOnline;
}
