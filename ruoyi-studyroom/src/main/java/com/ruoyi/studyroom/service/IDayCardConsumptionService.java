package com.ruoyi.studyroom.service;

import com.ruoyi.studyroom.domain.DayCardConsumption;
import com.ruoyi.studyroom.domain.vo.DayCardConsumptionVo;
import com.ruoyi.studyroom.domain.bo.DayCardConsumptionBo;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.core.domain.PageQuery;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * 日卡消费记录Service接口
 *
 * @author P_Peaceful
 * @date 2022-05-10
 */
public interface IDayCardConsumptionService {

    /**
     * 查询日卡消费记录
     *
     * @param id 日卡消费记录主键
     * @return 日卡消费记录
     */
    DayCardConsumptionVo queryById(Long id);

    /**
     * 查询日卡消费记录列表
     *
     * @param bo 日卡消费记录
     * @return 日卡消费记录集合
     */
    TableDataInfo<DayCardConsumptionVo> queryPageList(DayCardConsumptionBo bo, PageQuery pageQuery);

    /**
     * 查询日卡消费记录列表
     *
     * @param bo 日卡消费记录
     * @return 日卡消费记录集合
     */
    List<DayCardConsumptionVo> queryList(DayCardConsumptionBo bo);

    /**
     * 新增日卡消费记录
     *
     * @param bo 日卡消费记录
     * @return 结果
     */
    Boolean insertByBo(DayCardConsumptionBo bo);

    /**
     * 修改日卡消费记录
     *
     * @param bo 日卡消费记录
     * @return 结果
     */
    Boolean updateByBo(DayCardConsumptionBo bo);

    /**
     * 校验并批量删除日卡消费记录信息
     *
     * @param ids 需要删除的日卡消费记录主键集合
     * @param isValid 是否校验,true-删除前校验,false-不校验
     * @return 结果
     */
    Boolean deleteWithValidByIds(Collection<Long> ids, Boolean isValid);

    /**
     * 按天预约座位: 校验日卡有效性、当日重复预约、座位占用, 并写入日卡消费记录与座位占用记录
     *
     * @param bo 日卡消费记录
     * @return 结果
     */
    Boolean bookByDayCard(DayCardConsumptionBo bo);
    /**
     * 查询用户预约记录(含座位/门店/卡信息)
     *
     * @param userId 用户ID
     * @return 预约记录集合
     */
    List<DayCardConsumptionVo> queryMyRecords(Long userId);

    /**
     * 后台分页查询座位预约记录(含用户昵称/手机号/座位号/门店/卡券)
     *
     * @param pageQuery 分页参数
     * @param beginDate 开始日期(yyyy-MM-dd)
     * @param endDate   结束日期(yyyy-MM-dd)
     * @param nickname  用户昵称(模糊)
     * @param status    状态 0待使用 1已使用 2已失效
     * @param seatNum   座位号
     * @return 分页结果
     */
    TableDataInfo<DayCardConsumptionVo> queryBookingPage(PageQuery pageQuery, String beginDate, String endDate,
                                                        String nickname, Integer status, Integer seatNum);

    /**
     * 用户取消预约(日卡/周卡不退款, 长期卡按比例退余额)
     *
     * @param id     消费记录ID
     * @param userId 当前用户ID
     * @return 取消结果描述
     */
    String cancelByUser(Long id, Long userId);

    /**
     * 开灯签到: 记录用户当天首次开灯时间(用于连续签到/早到榜)
     *
     * @param seatId 座位ID
     * @param userId 用户ID
     * @return 是否记录了签到(首次)
     */
    boolean checkInByLightOn(Long seatId, Long userId);

    /**
     * 连续签到排行榜: 按连续签到天数降序
     *
     * @return 榜单(含 用户昵称/头像/连续天数/总签到天数)
     */
    List<Map<String, Object>> queryStreakRank();

    /**
     * 今日早到排行榜: 按当天首次开灯时间升序(谁先到谁靠前)
     *
     * @return 榜单(含 用户昵称/头像/签到时间)
     */
    List<Map<String, Object>> queryEarlyRank();

    /**
     * 月卡请假(停卡): 释放锁定的座位, 卡有效期顺延, 每张月卡最多请假2次
     *
     * @param cardUserId 卡用户ID
     * @param userId     当前用户ID
     * @return 请假结果描述
     */
    String leaveByCard(Long cardUserId, Long userId);

    /**
     * 周卡/月卡取消锁座(释放座位, 可重新预约; 不退款)
     *
     * @param cardUserId 卡用户ID
     * @param userId     当前用户ID
     * @return 取消锁座结果描述
     */
    String unlockByCard(Long cardUserId, Long userId);
}
