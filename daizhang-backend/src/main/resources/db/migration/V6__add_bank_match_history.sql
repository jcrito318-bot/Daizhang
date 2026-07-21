-- ============================================
-- V6__add_bank_match_history.sql - 银行匹配历史表
-- ============================================
-- 新增表:
--   1. bank_match_history - 银行匹配历史模式表
--     记录同一交易对方历史匹配的金额范围与对应科目,用于智能对账加分
-- ============================================

CREATE TABLE `bank_match_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `counterparty` VARCHAR(200) NOT NULL COMMENT '交易对方',
  `amount_range_min` DECIMAL(18,2) DEFAULT NULL COMMENT '金额范围最小值',
  `amount_range_max` DECIMAL(18,2) DEFAULT NULL COMMENT '金额范围最大值',
  `voucher_subject_code` VARCHAR(20) DEFAULT NULL COMMENT '对应凭证科目编码',
  `match_count` INT NOT NULL DEFAULT 1 COMMENT '历史匹配次数',
  `last_matched_at` DATETIME DEFAULT NULL COMMENT '最近匹配时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_counterparty` (`account_set_id`, `counterparty`),
  KEY `idx_match_history_account_set_id` (`account_set_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='银行匹配历史模式表';
