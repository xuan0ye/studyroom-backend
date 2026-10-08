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
     * 门禁刷脸签到: 记用户当天首次**到店**(门口刷脸)时间。
     *
     * <p>取代开灯签到作为签到口径。理由: 人必须真的站在门口, 是最难伪造的到店信号;
     * 而开灯依赖灯控链路(且可以被"到座位开个灯就走"糊弄)。</p>
     *
     * <p>只更新当天已存在的消费记录, <b>不新建</b> —— 新建会牵扯日卡扣次逻辑,
     * 误操作代价太大。找不到记录时只记日志(便于发现并后续补处理)。已签到则保留更早的时间。</p>
     *
     * @param userId    用户ID
     * @param eventTime 设备给出的通行时间(为空则用当前时间)
     * @return 本次是否真的写入了签到
     */
    boolean checkInByDoor(Long userId, java.util.Date eventTime);

    /**
     * 连续签到排行榜: 按连续签到天数降序
     *
     * @return 榜单(含 用户昵称/头像/连续天数/总签到天数)
     */
    List<Map<String, Object>> queryStreakRank();

    /**
     * 累计学习天数排行榜: 按累计签到天数降序。
     *
     * <p>取代原先的"学习时长榜"作为主榜。原因: 时长类指标要么鼓励"耗着不走"，
     * 要么只反映"买下了多久"，都不等于真的来学习；
     * 而<b>天数一天只可能 +1</b>，耗时间、开着灯不关都刷不出来，鼓励的是"天天来"。</p>
     *
     * @return 与 {@link #queryStreakRank()} 同源(同样的聚合口径)，只是排序换成按累计天数
     */
    List<Map<String, Object>> queryDaysRank();

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

    /**
     * 后台「释放座位」专用: 强制解除某张卡的锁座并清空锁座字段。
     *
     * <p>与用户端的「请假停卡 / 取消锁座」的区别是<b>不消耗请假次数</b>(后台的强制处置手段);
     * 但长期卡(月卡/季卡)仍然会冻结剩余天数 —— 与「取消锁座」同一口径,
     * 否则清掉锁座字段后下次预约会按卡面天数重新起算, 等于白送一个新周期。</p>
     *
     * @param cardUserId 卡用户ID(sr_card_user.id)
     * @return 结果描述; 该卡本来就没锁座时返回 null
     */
    String releaseCardLockByAdmin(Long cardUserId);
}
