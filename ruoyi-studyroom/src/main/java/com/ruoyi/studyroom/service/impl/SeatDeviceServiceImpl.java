package com.ruoyi.studyroom.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.studyroom.domain.SeatDevice;
import com.ruoyi.studyroom.domain.bo.SeatDeviceBo;
import com.ruoyi.studyroom.domain.vo.SeatDeviceVo;
import com.ruoyi.studyroom.mapper.SeatDeviceMapper;
import com.ruoyi.studyroom.service.ISeatDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 座位设备 Service 实现
 */
@Service
@RequiredArgsConstructor
public class SeatDeviceServiceImpl implements ISeatDeviceService {

    private final SeatDeviceMapper seatDeviceMapper;

    @Override
    public List<SeatDeviceVo> queryList(SeatDeviceBo bo) {
        return seatDeviceMapper.queryList(bo);
    }

    @Override
    public SeatDeviceVo queryById(Long deviceId) {
        return seatDeviceMapper.selectVoById(deviceId);
    }

    @Override
    public Boolean insertByBo(SeatDeviceBo bo) {
        SeatDevice entity = BeanUtil.toBean(bo, SeatDevice.class);
        return seatDeviceMapper.insert(entity) > 0;
    }

    @Override
    public Boolean updateByBo(SeatDeviceBo bo) {
        SeatDevice entity = BeanUtil.toBean(bo, SeatDevice.class);
        return seatDeviceMapper.updateById(entity) > 0;
    }

    @Override
    public Boolean deleteById(Long deviceId) {
        return seatDeviceMapper.deleteById(deviceId) > 0;
    }

    @Override
    public List<SeatDeviceVo> queryBySeatId(Long seatId) {
        SeatDeviceBo bo = new SeatDeviceBo();
        bo.setSeatId(seatId);
        return seatDeviceMapper.queryList(bo);
    }

    @Override
    public List<SeatDeviceVo> queryByRoomId(Long roomId) {
        SeatDeviceBo bo = new SeatDeviceBo();
        bo.setRoomId(roomId);
        return seatDeviceMapper.queryList(bo);
    }
}
