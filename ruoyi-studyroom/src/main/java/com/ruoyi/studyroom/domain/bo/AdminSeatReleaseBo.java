package com.ruoyi.studyroom.domain.bo;

import lombok.Data;

/**
 * 后台「释放座位」参数
 * <p>
 * 背景: 后台的「代占座/锁座」会同时写 sr_seat_record(占座) + sr_seat_lock(锁座),
 * 而原来的「撤销权限」只把锁座置为已取消, 不删占座记录 —— 结果座位在座位图上仍然被占、
 * 别人也约不了。这个接口按"座位真的空出来"为目标, 一次把相关数据全清掉。
 *
 * @author P_Peaceful
 */
@Data
public class AdminSeatReleaseBo {

    /**
     * 按占座记录释放(sr_seat_record.id), 与 lockId/userId 三选一
     */
    private Long seatRecordId;

    /**
     * 按锁座记录释放(sr_seat_lock.id)
     */
    private Long lockId;

    /**
     * 按用户释放(sr_user.user_id)
     */
    private Long userId;

    /**
     * 配合 userId 使用: 只释放该座位(可空 = 该用户全部座位)
     */
    private Long seatId;

    /**
     * 限定自习室(可空)
     */
    private Long roomId;

    /**
     * 释放范围:
     * record  = 只释放选中的那一条占座记录
     * seatUser= 该用户在该座位上的全部"未结束"占用(默认, 对应"完全释放")
     * user    = 该用户全部"未结束"占用
     */
    private String scope;

    /**
     * 是否同时解除该用户在该座位上的卡锁座(长期卡会冻结剩余天数, 不占请假次数); 默认 true
     */
    private Boolean clearCardLock;

    /**
     * 是否同时撤销该用户在该自习室的门禁权限; 默认 true
     */
    private Boolean revokeAccess;

    /**
     * 操作原因(写入日志, 便于事后追溯谁释放的)
     */
    private String reason;
}