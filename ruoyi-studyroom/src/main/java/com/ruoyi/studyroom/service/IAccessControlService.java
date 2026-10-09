package com.ruoyi.studyroom.service;

import com.ruoyi.studyroom.domain.bo.AccessDeviceLoginBo;
import com.ruoyi.studyroom.domain.bo.AccessFaceUploadBo;
import com.ruoyi.studyroom.domain.vo.AccessProfileVo;

import java.util.Map;

public interface IAccessControlService {

    AccessProfileVo uploadFaceAndSync(Long userId, String userName, AccessFaceUploadBo bo);

    AccessProfileVo queryCurrentProfile(Long userId, Long roomId);

    Map<String, Object> buildDeviceLoginResponse(AccessDeviceLoginBo bo);

    void handleMqttDataReport(Map<String, Object> payload);

    boolean remoteOpenDoor(Long userId, Long roomId);

    /**
     * 「一键开门」当前是否真的可用(小程序据此决定要不要显示按钮)。
     *
     * <p>2026-10-09 结论: 本机设备<b>不支持</b>用服务器指令开门 —— 它跑的是
     * {@code protocolVersion=0}(时之盾)私有协议, 只订阅 {@code iot/cli/<type>/<sn>/eth},
     * <b>从未订阅</b>文档协议的 {@code cs/} 主题; 实测文档里的 action 回 {@code code:"7"}
     * (不认该动作)、cs 的 cmd 报文回 {@code code:"2"}(解析失败)、
     * {@code open_door}/{@code read_param} 回 {@code code:"1"}(拒绝)。
     * 真正的远程开门由厂商云端签发 {@code {"code":"<不透明签名串>"}}, 密钥在厂商手里。</p>
     *
     * <p>因此仅在下面任一条成立时才返回 true, 否则前端<b>不显示按钮</b>
     * (免得给用户一个"点了却开不了门"的按钮):</p>
     * <ol>
     *   <li>已配置开门执行器(继电器/本地桥) {@code studyroom.door.open-url};</li>
     *   <li>设备通道自报 {@code supportsRemoteOpen()}(把设备协议改成 1 并重启后,
     *       由 {@code studyroom.door.mqtt-open-enabled} 打开)。</li>
     * </ol>
     */
    boolean doorOpenAvailable();

    /**
     * 预约结束后撤销门禁权限
     */
    void revokeAccessByOrder(String orderId, Long userId, Long roomId);

    /**
     * 门禁撤销失败补偿重试
     *
     * @return 本次处理数量
     */
    int compensateFailedRevokes();

    /**
     * 参观券到期 → 撤销门禁(删掉设备上的人脸)。
     *
     * <p>参观券的门禁窗口很短(默认 24 小时), 到期必须尽快删脸, 否则"参观完还能接着刷脸进来"。
     * 只处理 ① 凭证来自参观券({@code visit:} 前缀)且已过期, ② 用户当前没有其它有效凭证
     * (预约/新买的券) 的档案 —— 少一条都不删, 免得把人挡在门外。</p>
     *
     * <p>未配置参观券卡券 ID 时直接返回 0(功能关闭)。</p>
     *
     * @return 本次处理数量
     */
    int revokeExpiredVisitTickets();
}
