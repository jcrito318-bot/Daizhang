-- ============================================
-- V14__add_notification_table.sql - B3/B7 通用站内信通知表
-- ============================================
-- sys_notification 表:站内信通知,支撑催收预警/合同到期预警/工资单/系统广播等场景
--   - user_id 为 null 表示全员广播
--   - account_set_id 为 null 表示非账套级通知
--   - customer_id 为 null 表示非客户级通知
--   - type: ARREARS_WARNING/CONTRACT_EXPIRING/PAYSHEET/SYSTEM
--   - level: INFO/WARN/URGENT
--   - status: 0-未读 1-已读
-- 索引在表外创建,H2 与 MySQL 均兼容 CREATE INDEX IF NOT EXISTS 语法
-- ============================================

CREATE TABLE IF NOT EXISTS `sys_notification` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT COMMENT '接收人用户ID(null=全员广播)',
  `account_set_id` BIGINT COMMENT '关联账套ID(null=非账套级)',
  `customer_id` BIGINT COMMENT '关联客户ID(null=非客户级)',
  `type` VARCHAR(50) NOT NULL COMMENT '通知类型: ARREARS_WARNING/CONTRACT_EXPIRING/PAYSHEET/SYSTEM',
  `title` VARCHAR(200) NOT NULL COMMENT '通知标题',
  `content` TEXT COMMENT '通知内容',
  `level` VARCHAR(20) DEFAULT 'INFO' COMMENT '级别: INFO/WARN/URGENT',
  `status` INT DEFAULT 0 COMMENT '状态: 0-未读 1-已读',
  `create_by` BIGINT COMMENT '创建人',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT COMMENT '更新人',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '逻辑删除',
  `version` INT DEFAULT 0 COMMENT '乐观锁',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='站内信通知表';

CREATE INDEX IF NOT EXISTS `idx_notification_user` ON `sys_notification`(`user_id`);
CREATE INDEX IF NOT EXISTS `idx_notification_type` ON `sys_notification`(`type`);
CREATE INDEX IF NOT EXISTS `idx_notification_status` ON `sys_notification`(`status`);
