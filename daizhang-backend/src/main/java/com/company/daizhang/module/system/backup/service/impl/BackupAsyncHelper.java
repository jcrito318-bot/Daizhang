package com.company.daizhang.module.system.backup.service.impl;

import com.company.daizhang.module.system.backup.config.BackupProperties;
import com.company.daizhang.module.system.backup.entity.BackupRecord;
import com.company.daizhang.module.system.backup.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 备份异步执行助手 (P3.3)
 * <p>
 * 独立为 Component 的原因:Spring AOP 代理无法拦截同类内部方法调用(self-invocation),
 * 因此将 {@code @Async} 方法从 {@link BackupServiceImpl} 拆出,
 * 由 Service 注入本 Helper 后调用,确保走代理触发异步执行。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackupAsyncHelper {

    private final BackupService backupService;
    private final BackupProperties backupProperties;

    /**
     * 异步执行手动备份。
     * <p>
     * 实际工作委托给 {@link BackupService#executeBackup},此处仅承担异步代理入口职责。
     */
    @Async("backupAsyncExecutor")
    public void executeBackupAsync(Long recordId, String backupType, String triggerType, String remark) {
        backupService.executeBackup(recordId, backupType, triggerType, remark);
    }

    /**
     * 异步执行自动备份,完成后清理超期旧备份。
     */
    @Async("backupAsyncExecutor")
    public void executeAutoBackupAndCleanup(Long recordId, String backupType, String triggerType, String remark) {
        backupService.executeBackup(recordId, backupType, triggerType, remark);
        try {
            backupService.cleanupOldBackups(backupProperties.getMaxKeep());
        } catch (Exception e) {
            log.warn("清理超期旧备份失败", e);
        }
        // 清理后状态确认(in_progress 残留检测)
        BackupRecord record = backupService.getById(recordId);
        if (record != null && "in_progress".equals(record.getStatus())) {
            log.warn("自动备份记录仍处于 in_progress 状态,标记为 failed. recordId={}", recordId);
            record.setStatus("failed");
            record.setRemark("自动备份超时未完成");
            backupService.updateById(record);
        }
    }
}
