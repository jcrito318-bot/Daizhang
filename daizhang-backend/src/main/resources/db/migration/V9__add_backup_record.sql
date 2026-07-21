-- ============================================
-- V9__add_backup_record.sql - 备份记录表 (P3.3)
-- ============================================
-- 记录每次数据库备份的元信息,供备份管理界面查询/下载/恢复使用。
-- 字段说明:
--   file_name      - 备份文件名(含时间戳与类型)
--   file_path      - 备份文件绝对路径(便于下载时定位)
--   file_size      - 文件字节数,用于前端展示
--   backup_type    - 备份类型: full(全量) / incremental(增量,预留)
--   trigger_type   - 触发方式: manual(手动) / auto(定时任务)
--   status         - 备份状态: success / failed / in_progress
--   remark         - 备注(如失败原因或人工标注)
--   created_by     - 创建人ID(自动备份时可为 NULL)
--   created_by_name- 创建人名称(便于审计展示)
--   created_time   - 创建时间
--   deleted        - 逻辑删除标志
-- ============================================

CREATE TABLE IF NOT EXISTS `backup_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `file_name` VARCHAR(200) NOT NULL COMMENT '备份文件名',
  `file_path` VARCHAR(500) NOT NULL COMMENT '备份文件绝对路径',
  `file_size` BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
  `backup_type` VARCHAR(20) DEFAULT NULL COMMENT '备份类型: full/incremental',
  `trigger_type` VARCHAR(20) DEFAULT NULL COMMENT '触发方式: manual/auto',
  `status` VARCHAR(20) DEFAULT NULL COMMENT '状态: success/failed/in_progress',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `created_by` BIGINT DEFAULT NULL COMMENT '创建人ID',
  `created_by_name` VARCHAR(50) DEFAULT NULL COMMENT '创建人名称',
  `created_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  PRIMARY KEY (`id`),
  KEY `idx_created_time` (`created_time`),
  KEY `idx_status` (`status`),
  KEY `idx_trigger_type` (`trigger_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='数据库备份记录表';
