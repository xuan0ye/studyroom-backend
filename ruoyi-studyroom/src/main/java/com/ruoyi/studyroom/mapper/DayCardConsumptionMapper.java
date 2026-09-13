package com.ruoyi.studyroom.mapper;

import com.ruoyi.studyroom.domain.DayCardConsumption;
import com.ruoyi.studyroom.domain.vo.DayCardConsumptionVo;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 日卡消费记录Mapper接口
 *
 * @author P_Peaceful
 * @date 2022-05-10
 */
public interface DayCardConsumptionMapper extends BaseMapperPlus<DayCardConsumptionMapper, DayCardConsumption, DayCardConsumptionVo> {

    /**
     * 查询用户预约记录(含座位/门店/卡信息)
     */
    List<DayCardConsumptionVo> selectMyRecordList(@Param("userId") Long userId);

    /**
     * 后台分页查询座位预约记录(含用户昵称/手机号/座位号/门店/卡券)
     *
     * @param page      分页对象
     * @param beginDate 开始日期(yyyy-MM-dd)
     * @param endDate   结束日期(yyyy-MM-dd)
     * @param nickname  用户昵称(模糊)
     * @param status    状态 0待使用 1已使用 2已失效
     * @param seatNum   座位号
     */
    Page<DayCardConsumptionVo> selectBookingPage(Page<DayCardConsumptionVo> page,
                                                 @Param("beginDate") String beginDate,
                                                 @Param("endDate") String endDate,
                                                 @Param("nickname") String nickname,
                                                 @Param("status") Integer status,
                                                 @Param("seatNum") Integer seatNum);

}