package com.ruoyi.studyroom.domain.vo;

import com.alibaba.excel.annotation.ExcelIgnoreUnannotated;
import com.alibaba.excel.annotation.ExcelProperty;
import com.ruoyi.common.annotation.ExcelDictFormat;
import com.ruoyi.common.convert.ExcelDictConvert;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import java.util.Date;



/**
 * 自习室配置视图对象 sr_room_config
 *
 * @author P_Peaceful
 * @date 2022-05-10
 */
@Data
@ApiModel("自习室配置视图对象")
@ExcelIgnoreUnannotated
public class RoomConfigVo {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @ExcelProperty(value = "主键ID")
    @ApiModelProperty("主键ID")
    private Long id;

    /**
     * 自习室ID
     */
    @ExcelProperty(value = "自习室ID")
    @ApiModelProperty("自习室ID")
    private Long roomId;

    /**
     * 配置键
     */
    @ExcelProperty(value = "配置键")
    @ApiModelProperty("配置键")
    private String configKey;

    /**
     * 配置值
     */
    @ExcelProperty(value = "配置值")
    @ApiModelProperty("配置值")
    private String configValue;

    /**
     * 备注
     */
    @ExcelProperty(value = "备注")
    @ApiModelProperty("备注")
    private String remark;

}
