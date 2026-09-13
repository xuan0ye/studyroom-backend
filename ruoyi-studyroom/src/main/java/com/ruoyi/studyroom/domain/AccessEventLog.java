package com.ruoyi.studyroom.domain;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.ruoyi.common.core.domain.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sr_access_event_log")
public class AccessEventLog extends BaseEntity {

    @TableId
    private Long id;

    private String sn;
    private String cmd;
    private Long jobnumber;
    private String userName;
    private Date eventTime;
    private BigDecimal temperature;
    private String picBase64;
    private String rawPayload;
}
