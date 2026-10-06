package com.ruoyi.studyroom.service;

import java.util.Map;

/**
 * 门禁设备实时通道抽象。
 *
 * <p>按商家对接文档, 门禁机是"主动外连的客户端": 它连到我们服务器的长连接上,
 * 服务器沿这条连接下发命令, 设备回 result/errmsg 回执。因此下发必须"发到连接上并等回执",
 * 而不是"往一个频道里丢消息"(丢进去没人收也无人知晓, 这正是此前 sync_status 假成功的根源)。</p>
 *
 * <p>实现由 ruoyi-studyroom-wx 的 WebSocket 端点提供; 接口留在核心模块, 避免 wx 模块被反向依赖。</p>
 *
 * @author studyroom
 */
public interface IDeviceChannel {

    /** 该 sn 的设备当前是否有活连接(建连且近期有活跃) */
    boolean isOnline(String sn);

    /** 该 sn 最后一次活跃(建连/心跳/上报)的毫秒时间戳; 从未活跃返回 null */
    Long lastSeenMillis(String sn);

    /** 当前已连接的设备序列号列表(用于后台展示) */
    java.util.Set<String> onlineDevices();

    /**
     * 下发命令并等待设备回执。
     *
     * @param sn        设备序列号
     * @param payload   完整命令报文(含 cmd / sn / token / body)
     * @param timeoutMs 等待回执超时(毫秒)
     * @return 设备回执 Map(含 result/errmsg); <b>null 表示设备不在线或超时未回</b>
     */
    Map<String, Object> sendAndAwait(String sn, Map<String, Object> payload, long timeoutMs);

    /**
     * 把一条设备上报回灌给等待中的下发, 唤醒 {@link #sendAndAwait} 的等待方。
     *
     * <p>远程开门的结果确认由设备经「开门记录上传」(HTTP {@code url/upRecord}) 带回 token,
     * <b>不走 MQTT</b>, 因此 HTTP 入口需要这条回灌通道, 否则永远等不到回执。</p>
     *
     * @param sn      设备序列号
     * @param token   下发时给出的会话标识(设备原样带回)
     * @param payload 完整上报报文
     * @return true 表示确实有等待方被唤醒
     */
    default boolean completePending(String sn, String token, Map<String, Object> payload) {
        return false;
    }

    /**
     * 本通道能否真正下发"远程开门"。
     *
     * <p>默认 true(标准 WebSocket 协议里有 F1openDoor)。但走厂商 action 协议的设备
     * <b>必须返回 false</b>: 该固件只把 {@code open_door} 当作"门已开"的<b>上报事件</b>,
     * 服务器发同名指令会被拒({@code code:"1"}); 真正的远程开门由厂商云端用签名串
     * {@code {"code":"<blob>"}} 授权, 第三方无法伪造。宁可如实拒绝, 也不要给用户
     * 一个"提示开门成功但门没动"的按钮。</p>
     */
    default boolean supportsRemoteOpen() {
        return true;
    }

    /**
     * 本通道是否支持"设备原生人员管理"(在门禁机上建人员/下发人脸/删除人员)。
     *
     * <p>厂商《云端MQTT协议精简版 v1.12》那套 {@code cmd}/{@code F1redactUser} 协议
     * <b>这台固件并不认</b>(实测回 {@code {"action":"","data":[{"code":"2"}]}} —— 未知动作)。
     * 它认的是 action 协议里的 {@code add_user}/{@code add_face}/{@code read_user}/{@code del_user},
     * 字段格式已从厂商 broker 真实流量中还原并在我方设备上实测成功。</p>
     */
    default boolean supportsNativeUserSync() {
        return false;
    }

    /**
     * 在设备上创建人员并下发人脸。
     *
     * @param userAcct        人员账号(建议用我们自己的稳定标识, 便于后续按账号比对)
     * @param userName        姓名(设备屏幕会显示)
     * @param faceBase64Jpeg  人脸 JPG 的 base64(<b>不要带 data: 前缀</b>); 传空表示只建人员不下发人脸
     * @param validUntilSec   有效期截止(unix 秒); null/0 表示永久有效
     * @return 成功返回 {@code {user_id, user_acct, face_ok, face_code}}; 失败返回 null
     */
    default Map<String, Object> enrollUser(String sn, String userAcct, String userName,
                                            String faceBase64Jpeg, Long validUntilSec) {
        return null;
    }

    /** 读取设备上的人员列表(自动翻页); 失败返回空列表 */
    default java.util.List<Map<String, Object>> listDeviceUsers(String sn) {
        return java.util.Collections.emptyList();
    }

    /** 删除设备上指定的 user_id */
    default boolean deleteDeviceUser(String sn, String deviceUserId) {
        return false;
    }

    /**
     * 读取设备上的人脸(分页)。
     *
     * <p>实测报文: 请求 {@code {"action":"read_face","page":"1","size":"100","data":[]}},
     * 返回 {@code [{"face_id","user_id","face_img":"<base64 JPEG ~3.5KB>","feature":"<1368字符特征值>"}]}。
     * 设备返回的 {@code face_img} 本身就是小图, 可直接当缩略图用。</p>
     *
     * <p><b>注意</b>: 人脸属敏感个人信息, 调用方只应<b>转发展示</b>, 不得落库落盘。</p>
     *
     * @param page 起始页(从 1 开始)
     * @param size 每页张数(设备上限 100)
     * @return 人脸项列表(不含 code 项); 失败返回空列表
     */
    default java.util.List<Map<String, Object>> listDeviceFaces(String sn, int page, int size) {
        return java.util.Collections.emptyList();
    }
}
