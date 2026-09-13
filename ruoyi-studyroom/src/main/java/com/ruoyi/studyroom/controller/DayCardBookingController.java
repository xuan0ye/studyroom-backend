package com.ruoyi.studyroom.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.PageQuery;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.studyroom.domain.vo.DayCardConsumptionVo;
import com.ruoyi.studyroom.service.IDayCardConsumptionService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台-座位预约记录(卡预约: 日卡/周卡/月卡等)
 *
 * 说明: 卡预约不写 sr_order(余额下单才写), 后台「订单管理」看不到,
 * 这里单独提供按用户昵称/日期/座位号/状态查询的预约记录列表。
 *
 * @author ruoyi
 */
@Validated
@Api(value = "后台-预约记录", tags = {"后台-预约记录"})
@RequiredArgsConstructor
@RestController
@RequestMapping("/studyroom/booking")
public class DayCardBookingController extends BaseController {

    private final IDayCardConsumptionService dayCardConsumptionService;

    /**
     * 预约记录列表
     */
    @ApiOperation("预约记录列表(卡预约, 含用户昵称/手机号/座位号/卡券)")
    @SaCheckPermission("studyroom:booking:list")
    @GetMapping("/list")
    public TableDataInfo<DayCardConsumptionVo> list(@RequestParam(required = false) String beginDate,
                                                    @RequestParam(required = false) String endDate,
                                                    @RequestParam(required = false) String nickname,
                                                    @RequestParam(required = false) Integer status,
                                                    @RequestParam(required = false) Integer seatNum,
                                                    PageQuery pageQuery) {
        return dayCardConsumptionService.queryBookingPage(pageQuery, beginDate, endDate, nickname, status, seatNum);
    }
}
