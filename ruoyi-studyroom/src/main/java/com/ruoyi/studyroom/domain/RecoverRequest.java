package com.ruoyi.studyroom.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 补签申请 sr_recover_request
 */
@Data
@TableName("sr_recover_request")
public class RecoverRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String userName;

    private Date missedDate;

    private String reason;

    private String contact;

    private Integer status;

    private String remark;

    private Date createTime;

    private Date handleTime;

    private String handler;
}
