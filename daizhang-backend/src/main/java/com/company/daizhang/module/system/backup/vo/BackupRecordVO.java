package com.company.daizhang.module.system.backup.vo;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 备份记录视图对象 (P3.3)
 */
@Data
public class BackupRecordVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    /**
     * 备份文件名
     */
    private String fileName;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 创建人ID
     */
    private Long createdBy;

    /**
     * 创建人名称
     */
    private String createdByName;

    /**
     * 触发方式: manual(手动) / auto(自动)
     */
    private String type;

    /**
     * 备份状态: success / failed / in_progress
     */
    private String status;

    /**
     * 备份类型: full / incremental
     */
    private String backupType;

    /**
     * 备注
     */
    private String remark;
}
