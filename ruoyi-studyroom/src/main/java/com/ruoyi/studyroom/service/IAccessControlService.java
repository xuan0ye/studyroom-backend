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
     * 预约结束后撤销门禁权限
     */
    void revokeAccessByOrder(String orderId, Long userId, Long roomId);

    /**
     * 门禁撤销失败补偿重试
     *
     * @return 本次处理数量
     */
    int compensateFailedRevokes();
}
