-- ============================================
-- V11__add_user_totp.sql - 2FA TOTP 双因素认证 (P4.2)
-- ============================================
-- 1. user_totp 表:存储用户的 TOTP 密钥与备用恢复码
--    - secret: Base32 编码的 TOTP 密钥(RFC 6238 / Google Authenticator 兼容)
--    - backup_codes: JSON 数组字符串,10 个一次性备用码(明文比对,简化方案)
-- 2. sys_user 扩展 2FA 字段(实体已声明,此处补齐数据库列)
-- ============================================

CREATE TABLE IF NOT EXISTS `user_totp` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `secret` VARCHAR(64) NOT NULL COMMENT 'TOTP 密钥(Base32)',
  `enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用 0=未启用 1=已启用',
  `backup_codes` VARCHAR(500) DEFAULT NULL COMMENT '备用恢复码 JSON',
  `enabled_at` TIMESTAMP NULL DEFAULT NULL COMMENT '启用时间',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户 TOTP 双因素认证表';

-- sys_user 扩展 2FA 字段(实体 SysUser 已声明,补齐数据库列,避免查询报"列不存在")
ALTER TABLE `sys_user` ADD COLUMN `two_factor_enabled` TINYINT NOT NULL DEFAULT 0 COMMENT '是否启用2FA 0-未启用 1-已启用';
ALTER TABLE `sys_user` ADD COLUMN `two_factor_secret` VARCHAR(512) DEFAULT NULL COMMENT '2FA TOTP密钥(加密存储,预留)';
ALTER TABLE `sys_user` ADD COLUMN `two_factor_backup_codes` VARCHAR(512) DEFAULT NULL COMMENT '2FA备用恢复码(加密存储,预留)';
