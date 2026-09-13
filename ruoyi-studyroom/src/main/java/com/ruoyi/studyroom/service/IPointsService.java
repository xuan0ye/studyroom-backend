package com.ruoyi.studyroom.service;

import com.ruoyi.studyroom.domain.vo.PointsVo;
import com.ruoyi.studyroom.domain.vo.PointsLogVo;

import java.util.List;

/**
 * 积分服务
 */
public interface IPointsService {

    /**
     * 查询用户积分账户
     */
    PointsVo getByUserId(Long userId);

    /**
     * 签到加分(每天仅首次签到加一次, 积分按连续签到天数递增: 100起每天+10, 第11天起200封顶)
     *
     * @param userId 用户ID
     * @return 本次加了多少积分(0=今天已加过或未签到)
     */
    int addByCheckIn(Long userId);

    /**
     * 积分商城兑换日卡(3000积分=1张日卡)
     *
     * @param userId 用户ID
     * @return 新卡ID
     */
    Long exchangeDayCard(Long userId);

    /**
     * 提交补签申请(断签后好评截图给客服)
     *
     * @param userId     用户ID
     * @param missedDate 漏签日期
     * @param reason     补签说明
     * @param contact    联系方式
     * @return 申请ID
     */
    Long submitRecover(Long userId, String missedDate, String reason, String contact);

    /**
     * 客服审核补签(通过后补发积分并恢复连续)
     *
     * @param id     申请ID
     * @param pass   是否通过
     * @param remark 审核备注
     * @param handler 审核人
     */
    void handleRecover(Long id, boolean pass, String remark, String handler);

    /**
     * 审核通过好评发放积分(客服操作)
     *
     * @param userId   用户ID
     * @param points   积分
     * @param remark   备注
     * @param operator 操作人
     */
    void grantByAdmin(Long userId, int points, String remark, String operator);

    /**
     * 消费积分(兑换/抵扣)
     *
     * @param userId 用户ID
     * @param points 消耗积分
     * @param type   类型 3兑换 4抵扣
     * @param remark 备注
     */
    void spend(Long userId, int points, int type, String remark);

    /**
     * 查询积分流水
     */
    List<PointsLogVo> listLogs(Long userId, int limit);
}
