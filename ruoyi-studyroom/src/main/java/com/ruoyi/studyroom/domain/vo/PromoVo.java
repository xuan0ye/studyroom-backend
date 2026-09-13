package com.ruoyi.studyroom.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Date;

/**
 * 首页热门活动视图对象 sr_promo
 *
 * @author ruoyi
 */
@Data
@ApiModel("首页热门活动视图对象")
@ExcelIgnoreUnannotated
public class PromoVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @ExcelProperty(value = "主键")
    @ApiModelProperty("主键")
    private Long id;

    /**
     * 活动标题
     */
    @ExcelProperty(value = "活动标题")
    @ApiModelProperty("活动标题")
    private String title;

    /**
     * 副标题说明
     */
    @ExcelProperty(value = "副标题说明")
    @ApiModelProperty("副标题说明")
    private String subtitle;

    /**
     * 现价(元)
     */
    @ExcelProperty(value = "现价(元)")
    @ApiModelProperty("现价(元)")
    private BigDecimal price;

    /**
     * 原价(元)
     */
    @ExcelProperty(value = "原价(元)")
    @ApiModelProperty("原价(元)")
    private BigDecimal originalPrice;

    /**
     * 价格单位(如 元/人)
     */
    @ExcelProperty(value = "价格单位")
    @ApiModelProperty("价格单位")
    private String priceUnit;

    /**
     * 角标(如 2人团)
     */
    @ExcelProperty(value = "角标")
    @ApiModelProperty("角标")
    private String tag;

    /**
     * 配图地址
     */
    @ExcelProperty(value = "配图地址")
    @ApiModelProperty("配图地址")
    private String imageUrl;

    /**
     * 点击跳转路径
     */
    @ExcelProperty(value = "跳转路径")
    @ApiModelProperty("跳转路径")
    private String linkUrl;

    /**
     * 排序, 越小越前
     */
    @ExcelProperty(value = "排序")
    @ApiModelProperty("排序")
    private Integer sort;

    /**
     * 状态 1上架 0下架
     */
    @ExcelProperty(value = "状态")
    @ApiModelProperty("状态")
    private Integer status;

    /**
     * 备注
     */
    @ApiModelProperty("备注")
    private String remark;

    /**
     * 创建时间
     */
    @ApiModelProperty("创建时间")
    private Date createTime;

}
