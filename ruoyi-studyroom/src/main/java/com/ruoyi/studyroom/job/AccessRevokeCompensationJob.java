package com.ruoyi.studyroom.job;

import com.ruoyi.studyroom.service.IAccessControlService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 门禁撤销失败补偿任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AccessRevokeCompensationJob {

    private final IAccessControlService accessControlService;

    @Scheduled(cron = "${studyroom.access.revoke.cron:0 */2 * * * ?}")
    public void compensate() {
        int affected = accessControlService.compensateFailedRevokes();
        if (affected > 0) {
            log.info("门禁撤销补偿完成 affected={}", affected);
        }
    }
}
