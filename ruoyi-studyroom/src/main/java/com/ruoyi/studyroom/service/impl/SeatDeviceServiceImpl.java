package com.ruoyi.studyroom.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.ruoyi.studyroom.domain.SeatDevice;
import com.ruoyi.studyroom.domain.bo.SeatDeviceBo;
import com.ruoyi.studyroom.domain.vo.SeatDeviceVo;
import com.ruoyi.studyroom.mapper.SeatDeviceMapper;
import com.ruoyi.studyroom.service.ISeatDeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SeatDeviceServiceImpl implements ISeatDeviceService {

    private final SeatDeviceMapper seatDeviceMapper;

    @Override
    public List<SeatDeviceVo> queryList(SeatDeviceBo bo) {
        LambdaQueryWrapper<SeatDevice> lqw = buildQueryWrapper(bo);
        return seatDeviceMapper.selectVoList(lqw);
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
        return queryList(bo);
    }

    @Override
    public List<SeatDeviceVo> queryByRoomId(Long roomId) {
        SeatDeviceBo bo = new SeatDeviceBo();
        bo.setRoomId(roomId);
        return queryList(bo);
    }

    private LambdaQueryWrapper<SeatDevice> buildQueryWrapper(SeatDeviceBo bo) {
        LambdaQueryWrapper<SeatDevice> lqw = new LambdaQueryWrapper<>();
        if (bo.getSeatId() != null) {
            lqw.eq(SeatDevice::getSeatId, bo.getSeatId());
        }
        if (bo.getRoomId() != null) {
            lqw.eq(SeatDevice::getRoomId, bo.getRoomId());
        }
        if (bo.getDeviceUid() != null) {
            lqw.eq(SeatDevice::getDeviceUid, bo.getDeviceUid());
        }
        return lqw;
    }
}
