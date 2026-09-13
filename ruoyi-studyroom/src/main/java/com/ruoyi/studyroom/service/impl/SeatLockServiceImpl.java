package com.ruoyi.studyroom.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.ruoyi.studyroom.domain.bo.SeatLockBo;
import com.ruoyi.studyroom.domain.vo.SeatLockVo;
import com.ruoyi.studyroom.domain.SeatLock;
import com.ruoyi.studyroom.mapper.SeatLockMapper;
import com.ruoyi.studyroom.service.ISeatLockService;
import com.ruoyi.studyroom.service.ISeatService;
import com.ruoyi.studyroom.domain.vo.SeatVo;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Collection;

/**
 * 座位预占/锁定Service业务层处理
 *
 * @author P_Peaceful
 * @date 2022-05-10
 */
@RequiredArgsConstructor
@Service
public class SeatLockServiceImpl implements ISeatLockService {


    private final SeatLockMapper baseMapper;
    private final ISeatService seatService;
    /**
     * 查询座位预占/锁定
     *
     * @param id 座位预占/锁定主键
     * @return 座位预占/锁定
     */
    @Override
    public SeatLockVo queryById(Long id){
        return baseMapper.selectVoById(id);
    }

    /**
     * 查询座位预占/锁定列表
     *
     * @param bo 座位预占/锁定
     * @return 座位预占/锁定
     */
    @Override
    public TableDataInfo<SeatLockVo> queryPageList(SeatLockBo bo, PageQuery pageQuery) {
        LambdaQueryWrapper<SeatLock> lqw = buildQueryWrapper(bo);
        Page<SeatLockVo> result = baseMapper.selectVoPage(pageQuery.build(), lqw);
        return TableDataInfo.build(result);
    }

    /**
     * 查询座位预占/锁定列表
     *
     * @param bo 座位预占/锁定
     * @return 座位预占/锁定
     */
    @Override
    public List<SeatLockVo> queryList(SeatLockBo bo) {
        LambdaQueryWrapper<SeatLock> lqw = buildQueryWrapper(bo);
        List<SeatLockVo> list = baseMapper.selectVoList(lqw);
        for (SeatLockVo vo : list) {
            if (vo.getSeatId() != null) {
                SeatVo seat = seatService.queryById(vo.getSeatId());
                if (seat != null) {
                    vo.setSeatNum(seat.getSeatNum());
                }
            }
        }
        return list;
    }

    private LambdaQueryWrapper<SeatLock> buildQueryWrapper(SeatLockBo bo) {
        Map<String, Object> params = bo.getParams();
        LambdaQueryWrapper<SeatLock> lqw = Wrappers.lambdaQuery();
        lqw.eq(bo.getId() != null, SeatLock::getId, bo.getId());
        lqw.eq(bo.getUserId() != null, SeatLock::getUserId, bo.getUserId());
        lqw.eq(bo.getSeatId() != null, SeatLock::getSeatId, bo.getSeatId());
        lqw.eq(bo.getRoomId() != null, SeatLock::getRoomId, bo.getRoomId());
        lqw.eq(bo.getLockType() != null, SeatLock::getLockType, bo.getLockType());
        lqw.eq(bo.getStatus() != null, SeatLock::getStatus, bo.getStatus());
        lqw.eq(bo.getAutoRenew() != null, SeatLock::getAutoRenew, bo.getAutoRenew());
        lqw.ge(bo.getStartDate() != null, SeatLock::getStartDate, bo.getStartDate());
        lqw.le(bo.getEndDate() != null, SeatLock::getEndDate, bo.getEndDate());
        return lqw;
    }

    /**
     * 新增座位预占/锁定
     *
     * @param bo 座位预占/锁定
     * @return 结果
     */
    @Override
    public Boolean insertByBo(SeatLockBo bo) {
        SeatLock add = BeanUtil.toBean(bo, SeatLock.class);
        validEntityBeforeSave(add);
        boolean flag = baseMapper.insert(add) > 0;
        if (flag) {
            bo.setId(add.getId());
        }
        return flag;
    }

    /**
     * 修改座位预占/锁定
     *
     * @param bo 座位预占/锁定
     * @return 结果
     */
    @Override
    public Boolean updateByBo(SeatLockBo bo) {
        SeatLock update = BeanUtil.toBean(bo, SeatLock.class);
        validEntityBeforeSave(update);
        return baseMapper.updateById(update) > 0;
    }

    /**
     * 保存前的数据校验
     *
     * @param entity 实体类数据
     */
    private void validEntityBeforeSave(SeatLock entity){
        //TODO 做一些数据校验,如唯一约束
    }

    /**
     * 批量删除座位预占/锁定
     *
     * @param ids 需要删除的座位预占/锁定主键
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
     * 激活当天生效的座位锁定
     */
    @Override
    public void activateTodayLocks() {
        SeatLockBo bo = new SeatLockBo();
        bo.setStatus(0);
        List<SeatLockVo> list = queryList(bo);
        Date today = new Date();
        for (SeatLockVo vo : list) {
            if (vo.getStartDate() != null && !vo.getStartDate().after(today)) {
                SeatLockBo updateBo = new SeatLockBo();
                updateBo.setId(vo.getId());
                updateBo.setStatus(1);
                updateByBo(updateBo);
            }
        }
    }

    /**
     * 过期已过期的座位锁定
     */
    @Override
    public void expireOverdueLocks() {
        SeatLockBo bo = new SeatLockBo();
        bo.setStatus(1);
        List<SeatLockVo> list = queryList(bo);
        Date today = new Date();
        for (SeatLockVo vo : list) {
            if (vo.getEndDate() != null && vo.getEndDate().before(today)) {
                SeatLockBo updateBo = new SeatLockBo();
                updateBo.setId(vo.getId());
                updateBo.setStatus(2);
                updateByBo(updateBo);
            }
        }
    }
}
