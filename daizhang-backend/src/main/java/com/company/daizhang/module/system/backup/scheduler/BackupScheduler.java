package com.company.daizhang.module.system.backup.scheduler;

import com.company.daizhang.module.system.backup.config.BackupProperties;
import com.company.daizhang.module.system.backup.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 数据库备份定时任务 (P3.3)
 * <p>
 * 按 {@code app.backup.cron} 触发自动备份(默认每日凌晨 2 点)。
 * 通过 {@code app.backup.auto-enabled} 控制是否启用(默认 true)。
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.backup", name = "auto-enabled", havingValue = "true", matchIfMissing = true)
public class BackupScheduler {

    private final BackupService backupService;
    private final BackupProperties backupProperties;

    /**
     * 定时自动备份。cron 表达式由 {@code app.backup.cron} 配置,默认 {@code 0 0 2 * * ?}(每日凌晨 2 点)。
     * <p>
     * 实际备份异步执行,不阻塞调度线程。
     */
    @Scheduled(cron = "${app.backup.cron:0 0 2 * * ?}")
    public void scheduledAutoBackup() {
        log.info("定时备份任务触发, cron={}", backupProperties.getCron());
        try {
            backupService.triggerAutoBackup();
        } catch (Exception e) {
            log.error("定时备份任务异常", e);
        }
    }
}
