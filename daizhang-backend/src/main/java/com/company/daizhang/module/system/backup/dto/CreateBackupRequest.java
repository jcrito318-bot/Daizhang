package com.company.daizhang.module.system.backup.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建备份请求 (P3.3)
 */
@Data
public class CreateBackupRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 备份类型: full(全量) / incremental(增量,预留)
     * 默认 full
     */
    private String backupType = "full";

    /**
     * 备注
     */
    private String remark;
}
