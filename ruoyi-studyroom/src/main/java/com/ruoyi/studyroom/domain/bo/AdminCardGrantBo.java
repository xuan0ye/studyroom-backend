package com.ruoyi.studyroom.domain.bo;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.Date;

/**
 * 后台特殊卡发放请求
 *
 * @author studyroom
 */
@Data
@ApiModel("后台特殊卡发放请求")
public class AdminCardGrantBo {

    /**
     * 会员ID
     */
    @ApiModelProperty(value = "会员ID", required = true)
    @NotNull(message = "会员ID不能为空")
    private Long userId;

    /**
     * 卡ID(sr_card.id)
     */
    @ApiModelProperty(value = "卡ID", required = true)
    @NotNull(message = "卡ID不能为空")
    private Long cardId;

    /**
     * 到期时间(缺省=当前时间+卡term天)
     */
    @ApiModelProperty(value = "到期时间")
    private Date expiryTime;

    /**
     * 发放说明(记录用)
     */
    @ApiModelProperty(value = "发放说明")
    private String reason;
}
