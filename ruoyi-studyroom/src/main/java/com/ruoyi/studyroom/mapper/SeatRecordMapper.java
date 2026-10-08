package com.ruoyi.studyroom.mapper;

import com.ruoyi.studyroom.domain.SeatRecord;
import com.ruoyi.studyroom.domain.vo.SeatOccupationVo;
import com.ruoyi.studyroom.domain.vo.SeatRecordVo;
import com.ruoyi.common.core.mapper.BaseMapperPlus;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 座位-记录Mapper接口
 *
 * @author P_Peaceful
 * @date 2022-05-08
 */
public interface SeatRecordMapper extends BaseMapperPlus<SeatRecordMapper, SeatRecord, SeatRecordVo> {

    /**
     * 当前及未来的座位占用(带会员昵称/手机号、座位号、以及关联的手动锁座/卡锁座/日卡消费)
     * <p>
     * 供后台「释放座位」列表使用: 一行 = 一条未结束的 sr_seat_record。
     *
     * @param roomId 自习室ID(可空 = 全部)
     */
    List<SeatOccupationVo> selectOccupations(@Param("roomId") Long roomId);

}
