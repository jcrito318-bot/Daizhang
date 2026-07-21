package com.company.daizhang.module.system.backup.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 数据库备份记录实体 (P3.3)
 * <p>
 * 对应 backup_record 表,记录每次备份的元信息(文件名、路径、大小、类型、触发方式、状态等)。
 */
@Data
@TableName("backup_record")
public class BackupRecord implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 备份文件名(含时间戳与类型)
     */
    private String fileName;

    /**
     * 备份文件绝对路径(便于下载时定位)
     */
    private String filePath;

    /**
     * 文件大小(字节)
     */
    private Long fileSize;

    /**
     * 备份类型: full(全量) / incremental(增量,预留)
     */
    private String backupType;

    /**
     * 触发方式: manual(手动) / auto(定时任务)
     */
    private String triggerType;

    /**
     * 备份状态: success / failed / in_progress
     */
    private String status;

    /**
     * 备注(如失败原因或人工标注)
     */
    private String remark;

    /**
     * 创建人ID(自动备份时可为 NULL)
     */
    private Long createdBy;

    /**
     * 创建人名称(便于审计展示)
     */
    private String createdByName;

    /**
     * 创建时间
     */
    private LocalDateTime createdTime;

    /**
     * 逻辑删除标志: 0-未删除 1-已删除
     */
    @TableLogic
    private Integer deleted;
}
