package com.ruoyi.studyroom.service;

import com.ruoyi.studyroom.domain.SeatDevice;
import com.ruoyi.studyroom.domain.bo.SeatDeviceBo;
import com.ruoyi.studyroom.domain.vo.SeatDeviceVo;

import java.util.List;

/**
 * 座位设备 Service 接口
 */
public interface ISeatDeviceService {

    List<SeatDeviceVo> queryList(SeatDeviceBo bo);

    SeatDeviceVo queryById(Long deviceId);

    Boolean insertByBo(SeatDeviceBo bo);

    Boolean updateByBo(SeatDeviceBo bo);

    Boolean deleteById(Long deviceId);

    /** 根据座位ID查询关联设备 */
    List<SeatDeviceVo> queryBySeatId(Long seatId);

    /** 根据房间ID查询所有座位设备 */
    List<SeatDeviceVo> queryByRoomId(Long roomId);
}
