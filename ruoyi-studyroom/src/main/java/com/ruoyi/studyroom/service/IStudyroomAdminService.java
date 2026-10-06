package com.ruoyi.studyroom.service;

import com.ruoyi.studyroom.domain.bo.AdminCardGrantBo;
import com.ruoyi.studyroom.domain.bo.AdminCardUpdateBo;
import com.ruoyi.studyroom.domain.bo.AdminLeaveGrantBo;
import com.ruoyi.studyroom.domain.bo.AdminOccupyBo;
import com.ruoyi.studyroom.domain.bo.AdminRefundBo;
import com.ruoyi.studyroom.domain.bo.AdminRevokeBo;

/**
 * 自习室后台运营操作Service接口
 *
 * @author studyroom
 */
public interface IStudyroomAdminService {

    /**
     * 代占座/锁座: 管理员直接为用户锁定座位
     * 创建占座记录(sr_seat_record), 可选创建锁座记录(sr_seat_lock)
     *
     * @param bo 请求
     * @return 是否成功
     */
    Boolean occupy(AdminOccupyBo bo);

    /**
     * 撤销权限: 撤销门禁授权 + 使锁座记录失效
     *
     * @param bo 请求
     * @return 是否成功
     */
    Boolean revoke(AdminRevokeBo bo);

    /**
     * 退款: 订单金额退回用户余额
     *
     * @param bo 请求
     * @return 是否成功
     */
    Boolean refund(AdminRefundBo bo);

    /**
     * 特殊卡发放: 直接给用户发放一张卡
     *
     * @param bo 请求
     * @return 是否成功
     */
    Boolean grantCard(AdminCardGrantBo bo);

    /**
     * 特殊卡修改: 调整用户卡到期时间/请假次数/请假额度
     *
     * @param bo 请求
     * @return 是否成功
     */
    Boolean updateCard(AdminCardUpdateBo bo);

    /**
     * 发放请假次数: 在用户卡原有请假额度上追加次数(每张长期卡默认 3 次)
     *
     * @param bo 请求
     * @return 处理结果说明
     */
    String grantLeave(AdminLeaveGrantBo bo);

    /**
     * 删除会员卡券(后台): 释放锁座、删除未到期的座位占用、作废预约记录、撤销门禁权限后删除卡券
     *
     * @param cardUserId 用户卡记录ID(sr_card_user.id)
     * @param reason     删除原因(记录日志)
     * @return 处理结果说明
     */
    String revokeCard(Long cardUserId, String reason);
}
