package com.ruoyi.studyroom.job;

import com.ruoyi.studyroom.service.IAccessControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 参观券到期撤权任务。
 *
 * <p><b>为什么单独一个任务、周期还短(默认 5 分钟)</b>: 预约的撤权可以容忍"两小时内删掉"
 * (见 AccessRevokeCompensationJob, 它是每 2 小时一轮);
 * 参观券不行 —— 门禁窗口只有几小时, 到期后必须尽快把设备上的人脸删掉,
 * 否则"参观完还能接着刷脸进来", 等于白送长期权限。</p>
 *
 * <p>未配置参观券(studyroom.access.visit-card-id=0)时本任务空转, 不做任何查询与下发。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessVisitTicketJob {

    private final IAccessControlService accessControlService;

    @Scheduled(cron = "${studyroom.access.visit.revoke-cron:0 */5 * * * ?}")
    public void revokeExpired() {
        int affected = accessControlService.revokeExpiredVisitTickets();
        if (affected > 0) {
            log.info("[门禁] 参观券到期撤权完成 affected={}", affected);
        }
    }
}