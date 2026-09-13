package com.ruoyi.studyroom.domain.bo;

import lombok.Data;

@Data
public class AccessDeviceLoginBo {
    private String sn;
    private String signType;
    private String registerNo;
    private Integer type;
    private String name;
    private Boolean isFirst;
    private String deviceIp;
}
