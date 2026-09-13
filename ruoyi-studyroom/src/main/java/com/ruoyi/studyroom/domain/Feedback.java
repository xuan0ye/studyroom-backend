package com.ruoyi.studyroom.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 意见反馈 sr_feedback
 */
@Data
@TableName("sr_feedback")
public class Feedback implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String userName;

    private String content;

    private String contact;

    private Integer status;

    private String reply;

    private Date createTime;

    private Date handleTime;
}
