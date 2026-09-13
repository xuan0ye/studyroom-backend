package com.ruoyi.studyroom.service;

import com.ruoyi.studyroom.domain.SeatLock;
import com.ruoyi.studyroom.domain.vo.SeatLockVo;
import com.ruoyi.studyroom.domain.bo.SeatLockBo;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;

import java.util.Collection;
import java.util.List;

/**
 * 座位预占/锁定Service接口
 *
 * @author P_Peaceful
 * @date 2022-05-10
 */
public interface ISeatLockService {

    /**
     * 查询座位预占/锁定
     *
     * @param id 座位预占/锁定主键
     * @return 座位预占/锁定
     */
    SeatLockVo queryById(Long id);

    /**
     * 查询座位预占/锁定列表
     *
     * @param bo 座位预占/锁定
     * @return 座位预占/锁定集合
     */
    TableDataInfo<SeatLockVo> queryPageList(SeatLockBo bo, PageQuery pageQuery);

    /**
     * 查询座位预占/锁定列表
     *
     * @param bo 座位预占/锁定
     * @return 座位预占/锁定集合
     */
    List<SeatLockVo> queryList(SeatLockBo bo);

    /**
     * 新增座位预占/锁定
     *
     * @param bo 座位预占/锁定
     * @return 结果
     */
    Boolean insertByBo(SeatLockBo bo);

    /**
     * 修改座位预占/锁定
     *
     * @param bo 座位预占/锁定
     * @return 结果
     */
    Boolean updateByBo(SeatLockBo bo);

    /**
     * 校验并批量删除座位预占/锁定信息
     *
     * @param ids 需要删除的座位预占/锁定主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 结果
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 激活当天生效的座位锁定
     */
    void activateTodayLocks();

    /**
     * 过期已过期的座位锁定
     */
    void expireOverdueLocks();
}
