package com.company.daizhang.module.system.backup.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.daizhang.common.result.PageResult;
import com.company.daizhang.module.system.backup.dto.CreateBackupRequest;
import com.company.daizhang.module.system.backup.entity.BackupRecord;
import com.company.daizhang.module.system.backup.vo.BackupRecordVO;

/**
 * 数据备份服务接口 (P3.3)
 */
public interface BackupService extends IService<BackupRecord> {

    /**
     * 创建备份(异步执行)。
     * <p>
     * 立即创建一条 status=in_progress 的记录并返回 ID,实际备份在异步线程中执行。
     *
     * @param request 备份请求(含 backupType / remark)
     * @return 备份记录ID
     */
    Long createBackup(CreateBackupRequest request);

    /**
     * 异步执行实际的备份工作(生成 SQL 脚本 ZIP + 写入文件)。
     * <p>
     * 供 {@link #createBackup} 内部通过 {@code @Async} 调用,不直接暴露给 Controller。
     *
     * @param recordId    备份记录ID
     * @param backupType  备份类型: full / incremental
     * @param triggerType 触发方式: manual / auto
     * @param remark      备注
     */
    void executeBackup(Long recordId, String backupType, String triggerType, String remark);

    /**
     * 分页查询备份记录。
     */
    PageResult<BackupRecordVO> pageBackups(String backupType, String status, String triggerType,
                                            int pageNum, int pageSize);

    /**
     * 根据 ID 获取备份记录(不存在抛业务异常)。
     */
    BackupRecord getBackupByIdRequired(Long id);

    /**
     * 删除备份(同时删除物理文件与数据库记录)。
     */
    void deleteBackup(Long id);

    /**
     * 恢复备份。
     * <p>
     * 危险操作:会清空当前数据库并从备份 SQL 脚本恢复。恢复后建议重启应用。
     *
     * @param id      备份记录ID
     * @param confirm 是否已确认(必须为 true)
     */
    void restoreBackup(Long id, boolean confirm);

    /**
     * 触发自动备份(由定时任务调用),并在完成后清理超期旧备份。
     */
    void triggerAutoBackup();

    /**
     * 清理超期旧备份,保留最近的 maxKeep 份。
     *
     * @param maxKeep 最多保留份数
     */
    void cleanupOldBackups(int maxKeep);
}
