package com.ruoyi.studyroom.domain;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.math.BigDecimal;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 首页热门活动对象 sr_promo
 *
 * @author ruoyi
 */
@Data
@TableName("sr_promo")
public class Promo extends BaseEntity {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId(value = "id")
    private Long id;

    /**
     * 活动标题
     */
    private String title;

    /**
     * 副标题说明
     */
    private String subtitle;

    /**
     * 现价(元)
     */
    private BigDecimal price;

    /**
     * 原价(元)
     */
    private BigDecimal originalPrice;

    /**
     * 价格单位(如 元/人)
     */
    private String priceUnit;

    /**
     * 角标(如 2人团)
     */
    private String tag;

    /**
     * 配图地址
     */
    private String imageUrl;

    /**
     * 点击跳转路径
     */
    private String linkUrl;

    /**
     * 排序, 越小越前
     */
    private Integer sort;

    /**
     * 状态 1上架 0下架
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;

}
