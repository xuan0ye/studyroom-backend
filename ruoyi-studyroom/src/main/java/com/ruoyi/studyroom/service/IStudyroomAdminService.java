package com.ruoyi.studyroom.service;

import com.ruoyi.studyroom.domain.bo.AdminCardGrantBo;
import com.ruoyi.studyroom.domain.bo.AdminCardUpdateBo;
import com.ruoyi.studyroom.domain.bo.AdminLeaveGrantBo;
import com.ruoyi.studyroom.domain.bo.AdminOccupyBo;
import com.ruoyi.studyroom.domain.bo.AdminRefundBo;
import com.ruoyi.studyroom.domain.bo.AdminRevokeBo;
import com.ruoyi.studyroom.domain.bo.AdminSeatReleaseBo;

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
     * 当前座位占用列表(未结束的占座记录 + 关联的锁座/卡锁座/日卡预约)
     * <p>
     * 给后台「释放座位」用: 先让管理员看见"这个座位是被什么占着", 再决定释放哪一个。
     *
     * @param roomId 自习室ID(可空 = 全部)
     * @return 占用列表
     */
    java.util.List<com.ruoyi.studyroom.domain.vo.SeatOccupationVo> listOccupations(Long roomId);

    /**
     * 释放座位: 一次把"占着这个座位的东西"全部清掉 ——
     * 删占座记录(座位图与预约占用检查读的就是它) + 取消锁座记录 + 解除卡锁座(可选)
     * + 作废对应的日卡消费记录 + 撤销门禁权限(可选)。
     * <p>
     * 与原来的「撤销权限」区别: 撤销权限只把锁座置为已取消, 不删占座记录, 所以座位仍然被占。
     *
     * @param bo 请求(占座记录ID / 锁座记录ID / 用户ID 三选一)
     * @return 处理结果(各项清理条数 + 说明)
     */
    java.util.Map<String, Object> releaseSeat(com.ruoyi.studyroom.domain.bo.AdminSeatReleaseBo bo);

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
