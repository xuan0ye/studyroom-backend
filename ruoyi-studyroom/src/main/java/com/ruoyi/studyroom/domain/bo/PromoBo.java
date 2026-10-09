package com.ruoyi.studyroom.domain.bo;

import com.ruoyi.common.core.validate.AddGroup;
import com.ruoyi.common.core.validate.EditGroup;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 首页热门活动业务对象 sr_promo
 *
 * @author ruoyi
 */
@Data
@EqualsAndHashCode(callSuper = true)
@ApiModel("首页热门活动业务对象")
public class PromoBo extends BaseEntity {

    /**
     * 主键
     */
    @ApiModelProperty(value = "主键", required = true)
    @NotNull(message = "主键不能为空", groups = {EditGroup.class})
    private Long id;

    /**
     * 活动标题
     */
    @ApiModelProperty(value = "活动标题", required = true)
    @NotBlank(message = "活动标题不能为空", groups = {AddGroup.class, EditGroup.class})
    private String title;

    /**
     * 副标题说明
     */
    @ApiModelProperty(value = "副标题说明")
    private String subtitle;

    /**
     * 现价(元)
     */
    @ApiModelProperty(value = "现价(元)")
    private BigDecimal price;

    /**
     * 原价(元)
     */
    @ApiModelProperty(value = "原价(元)")
    private BigDecimal originalPrice;

    /**
     * 价格单位(如 元/人)
     */
    @ApiModelProperty(value = "价格单位, 如 元/人")
    private String priceUnit;

    /**
     * 角标(如 2人团)
     */
    @ApiModelProperty(value = "角标, 如 2人团")
    private String tag;

    /**
     * 配图地址。
     *
     * <p><b>为什么带自定义反序列化</b>: 后台「热门活动」表单用的是图片上传组件
     * {@code components/ImageUpload}, 它的 v-model <b>永远 emit 数组</b> ——
     * 于是有没有选图, 保存时 imageUrl 都是 {@code ["url"]} 或 {@code []},
     * 默认反序列化会报 {@code Cannot deserialize value of type java.lang.String from Array value}。
     * 交给 {@link FirstStringDeserializer} 归一成单个 URL(空数组 → null)。</p>
     */
    @ApiModelProperty(value = "配图地址")
    @com.fasterxml.jackson.databind.annotation.JsonDeserialize(using = FirstStringDeserializer.class)
    private String imageUrl;

    /**
     * 点击跳转路径
     */
    @ApiModelProperty(value = "点击跳转路径, 如 /other_pages/recharge-card/recharge-card")
    private String linkUrl;

    /**
     * 排序, 越小越前
     */
    @ApiModelProperty(value = "排序, 越小越前")
    private Integer sort;

    /**
     * 状态 1上架 0下架
     */
    @ApiModelProperty(value = "状态 1上架 0下架")
    private Integer status;

    /**
     * 备注
     */
    @ApiModelProperty(value = "备注")
    private String remark;

}
