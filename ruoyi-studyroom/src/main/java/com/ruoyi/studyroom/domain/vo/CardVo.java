package com.ruoyi.studyroom.domain.vo;

import java.math.BigDecimal;
import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;



/**
 * 优惠卡视图对象 sr_card
 *
 * @author P_Peaceful
 * @date 2022-05-08
 */
@Data
@ApiModel("优惠卡视图对象")
@ExcelIgnoreUnannotated
public class CardVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    @ApiModelProperty("主键ID")
    private Long id;

    /**
     * 优惠卡
     */
    @ExcelProperty(value = "优惠卡")
    @ApiModelProperty("优惠卡")
    private String cardTitle;

    /**
     * 卡券详细说明(小程序卡券页展示, 支持换行)
     */
    @ExcelProperty(value = "卡券说明")
    @ApiModelProperty("卡券详细说明")
    private String cardDesc;

    /**
     * 价格
     */
    @ExcelProperty(value = "价格")
    @ApiModelProperty("价格")
    private BigDecimal price;

    /**
     * 可用区域
     */
    @ExcelProperty(value = "可用区域", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "area_status")
    @ApiModelProperty("可用区域")
    private Integer usable;

    /**
     * 次数(天)
     */
    @ExcelProperty(value = "次数(天)")
    @ApiModelProperty("次数(天)")
    private Long frequency;

    /**
     * 有效期(天)
     */
    @ExcelProperty(value = "有效期(天)")
    @ApiModelProperty("有效期(天)")
    private Long term;

    /**
     * 状态
     */
    @ExcelProperty(value = "状态", converter = ExcelDictConvert.class)
    @ExcelDictFormat(dictType = "common_status")
    @ApiModelProperty("状态")
    private Integer status;

    /**
     * 区域限制: 0=不限区域 1=仅沉浸区(16-19号) 2=禁止沉浸区(16-19号)
     */
    @ApiModelProperty("区域限制")
    private Integer cardType;

    /**
     * 套餐人数: 1=单人 2=双人(购买后额外生成 1 个好友兑换码)
     */
    @ApiModelProperty("套餐人数")
    private Integer partySize;

    /**
     * 固定使用起始日(与 validEnd 同时存在时为"固定区间卡": 只能在区间内使用, 每天单独预约、不连锁锁座)
     */
    @ApiModelProperty("固定使用起始日")
    private Date validStart;

    /**
     * 固定使用截止日(含当天)
     */
    @ApiModelProperty("固定使用截止日")
    private Date validEnd;

    /**
     * 排序: 越小越靠前(0=最前)
     */
    @ApiModelProperty("排序(越小越靠前)")
    private Integer sort;

}
