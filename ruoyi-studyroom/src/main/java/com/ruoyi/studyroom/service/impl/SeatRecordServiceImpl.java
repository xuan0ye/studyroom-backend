package com.ruoyi.studyroom.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ruoyi.studyroom.domain.bo.SeatRecordBo;
import com.ruoyi.studyroom.domain.vo.SeatRecordVo;
import com.ruoyi.studyroom.domain.SeatRecord;
import com.ruoyi.studyroom.domain.User;
import com.ruoyi.studyroom.domain.Seat;
import com.ruoyi.studyroom.domain.Room;
import com.ruoyi.studyroom.mapper.SeatRecordMapper;
import com.ruoyi.studyroom.mapper.UserMapper;
import com.ruoyi.studyroom.mapper.SeatMapper;
import com.ruoyi.studyroom.mapper.RoomMapper;
import com.ruoyi.studyroom.service.ISeatRecordService;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Collection;
import java.util.function.Consumer;

/**
 * 座位-记录Service业务层处理
 *
 * @author P_Peaceful
 * @date 2022-05-08
 */
@RequiredArgsConstructor
@Service
public class SeatRecordServiceImpl implements ISeatRecordService {

    private final SeatRecordMapper baseMapper;
    private final UserMapper userMapper;
    private final SeatMapper seatMapper;
    private final RoomMapper roomMapper;

    /**
     * 补全关联展示字段(用户昵称/手机号, 座位号, 自习室名, 区域名)
     */
    private void fillDisplayFields(SeatRecordVo vo) {
        if (vo == null) {
            return;
        }
        if (vo.getUserId() != null) {
            User user = userMapper.selectById(vo.getUserId());
            if (user != null) {
                vo.setUserName(user.getNickname());
                vo.setUserPhone(user.getPhone());
            }
        }
        if (vo.getSeatId() != null) {
            Seat seat = seatMapper.selectById(vo.getSeatId());
            if (seat != null) {
                vo.setSeatNum(seat.getSeatNum());
                vo.setAreaName(seat.getAreaName());
            }
        }
        if (vo.getRoomId() != null) {
            Room room = roomMapper.selectById(vo.getRoomId());
            if (room != null) {
                vo.setRoomName(room.getRoomName());
            }
        }
    }

    /**
     * 查询座位-记录
     *
     * @param id 座位-记录主键
     * @return 座位-记录
     */
    @Override
    public SeatRecordVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询座位-记录列表
     *
     * @param bo 座位-记录
     * @return 座位-记录
     */
    @Override
    public TableDataInfo<SeatRecordVo> queryPageList(SeatRecordBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SeatRecord> lqw = buildQueryWrapper(bo);
        Page<SeatRecordVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        if (result.getRecords() != null) {
            result.getRecords().forEach(this::fillDisplayFields);
        }
        return TableDataInfo.build(result);
    }

    /**
     * 查询座位-记录列表
     *
     * @param bo 座位-记录
     * @return 座位-记录
     */
    @Override
    public List<SeatRecordVo> queryList(SeatRecordBo bo) {
        LambdaQueryWrapper<SeatRecord> lqw = buildQueryWrapper(bo);
        List<SeatRecordVo> list = baseMapper.selectVoList(lqw);
        if (list != null) {
            list.forEach(this::fillDisplayFields);
        }
        return list;
    }

    private LambdaQueryWrapper<SeatRecord> buildQueryWrapper(SeatRecordBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SeatRecord> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getRoomId() != null, SeatRecord::getRoomId, bo.getRoomId());
        lqw.eq(bo.getSeatId() != null, SeatRecord::getSeatId, bo.getSeatId());
        lqw.eq(bo.getUserId() != null, SeatRecord::getUserId, bo.getUserId());
        lqw.eq(bo.getStartTime() != null, SeatRecord::getStartTime, bo.getStartTime());
        lqw.eq(bo.getEndTime() != null, SeatRecord::getEndTime, bo.getEndTime());
        return lqw;
    }

    /**
     * 新增座位-记录
     *
     * @param bo 座位-记录
     * @return 结果
     */
    @Override
    public Boolean insertByBo(SeatRecordBo bo) {
        SeatRecord add = BeanUtil.toBean(bo, SeatRecord.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改座位-记录
     *
     * @param bo 座位-记录
     * @return 结果
     */
    @Override
    public Boolean updateByBo(SeatRecordBo bo) {
        SeatRecord update = BeanUtil.toBean(bo, SeatRecord.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(SeatRecord entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除座位-记录
     *
     * @param ids 需要删除的座位-记录主键
     * @return 结果
     */
    @Override
    public Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid) {
        if(isValid){
            //TODO 做一些业务上的校验,判断是否需要校验
        }
        return baseMapper.deleteBatchIds(ids) > 0;
    }

    /**
     * 查询不可以预定的座位ID
     *
     * @param bo
     * @return
     */
    @Override
    public List<SeatRecordVo> queryListByTime(SeatRecordBo bo) {
        if (bo.getStartTime() == null || bo.getEndTime() == null) {
            throw new ServiceException("开始/结束时间不能为空");
        }
        LambdaQueryWrapper<SeatRecord> lqw = Wrappers.lambdaQuery();
                if (bo.getRoomId() != null) {
            lqw.eq(SeatRecord::getRoomId, bo.getRoomId());
        }
        // 标准区间重叠判断: 记录开始时间 < 查询结束时间 且 记录结束时间 > 查询开始时间
        lqw.lt(SeatRecord::getStartTime, bo.getEndTime())
            .gt(SeatRecord::getEndTime, bo.getStartTime());
        return baseMapper.selectVoList(lqw);
    }
}
