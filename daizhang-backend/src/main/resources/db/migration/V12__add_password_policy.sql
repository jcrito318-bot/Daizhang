-- ============================================
-- V12__add_password_policy.sql - 密码策略 + 登录锁定 (P4.3)
-- ============================================
-- 1. password_history:密码历史,改密时记录旧密码 hash,新密码不能与最近 N 次相同
-- 2. login_attempt:登录尝试记录,用于登录锁定判定(15 分钟内失败 >=5 次则锁定)
-- 3. sys_user 扩展密码策略字段(实体 SysUser 已声明,补齐数据库列)
-- ============================================

CREATE TABLE IF NOT EXISTS `password_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `password_hash` VARCHAR(200) NOT NULL COMMENT '历史密码 hash(BCrypt)',
  `create_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='密码历史表';

CREATE TABLE IF NOT EXISTS `login_attempt` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(100) NOT NULL COMMENT '用户名',
  `attempt_time` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '尝试时间',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT '客户端IP',
  `success` TINYINT NOT NULL DEFAULT 0 COMMENT '是否成功 0-失败 1-成功',
  PRIMARY KEY (`id`),
  KEY `idx_username_time` (`username`, `attempt_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录尝试记录表';

-- sys_user 扩展密码策略字段(实体 SysUser 已声明,补齐数据库列)
ALTER TABLE `sys_user` ADD COLUMN `password_changed_at` TIMESTAMP NULL DEFAULT NULL COMMENT '最后改密时间';
ALTER TABLE `sys_user` ADD COLUMN `login_fail_count` INT NOT NULL DEFAULT 0 COMMENT '登录失败次数';
ALTER TABLE `sys_user` ADD COLUMN `locked_until` TIMESTAMP NULL DEFAULT NULL COMMENT '锁定截止时间';

-- 为存量用户补齐最后改密时间为创建时间(避免一上线就触发密码过期)
UPDATE `sys_user` SET `password_changed_at` = `create_time` WHERE `password_changed_at` IS NULL;
