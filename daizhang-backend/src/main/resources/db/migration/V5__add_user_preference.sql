-- ============================================
-- V5__add_user_preference.sql - 用户账套偏好表
-- ============================================
-- 新增表:
--   1. user_account_set_preference - 用户账套偏好 (收藏/最近访问/排序)
--     用于顶部账套切换器记忆最近访问 + 收藏置顶
-- ============================================

CREATE TABLE `user_account_set_preference` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `is_favorite` TINYINT NOT NULL DEFAULT 0 COMMENT '是否收藏 0-否 1-是',
  `last_accessed_at` DATETIME DEFAULT NULL COMMENT '最近访问时间',
  `access_count` INT NOT NULL DEFAULT 0 COMMENT '访问次数',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_account` (`user_id`, `account_set_id`),
  INDEX `idx_user_pref` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户账套偏好';
