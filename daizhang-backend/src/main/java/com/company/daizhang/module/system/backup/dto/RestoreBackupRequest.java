package com.company.daizhang.module.system.backup.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 恢复备份请求 (P3.3)
 * <p>
 * 恢复是危险操作,必须显式 confirm=true 才能执行。
 */
@Data
public class RestoreBackupRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 备份记录ID
     */
    private Long backupId;

    /**
     * 二次确认标志:必须为 true 才能执行恢复。
     * 该字段为业务层确认(独立于 X-Confirm 请求头),双重保险防止误操作。
     */
    private Boolean confirm = false;
}
