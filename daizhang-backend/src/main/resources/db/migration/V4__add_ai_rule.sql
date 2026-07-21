-- ============================================
-- V4__add_ai_rule.sql - AI 规则 + 反馈表 + 默认规则数据
-- ============================================
-- 新增表:
--   1. ai_accounting_rule       - AI记账规则库 (关键词->借贷科目映射)
--   2. ai_recognition_feedback  - AI识别反馈记录 (闭环学习)
-- 种子数据:
--   - 13 条默认全局 AI 记账规则 (account_set_id=0)
-- ============================================

CREATE TABLE `ai_accounting_rule` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL DEFAULT 0 COMMENT '账套ID, 0=全局规则',
  `keyword` VARCHAR(100) NOT NULL COMMENT '业务关键词,如 差旅费/办公费/工资',
  `debit_subject_code` VARCHAR(20) NOT NULL COMMENT '借方科目编码',
  `debit_subject_name` VARCHAR(100) DEFAULT NULL COMMENT '借方科目名称',
  `credit_subject_code` VARCHAR(20) NOT NULL COMMENT '贷方科目编码',
  `credit_subject_name` VARCHAR(100) DEFAULT NULL COMMENT '贷方科目名称',
  `voucher_summary` VARCHAR(200) DEFAULT NULL COMMENT '建议摘要',
  `priority` INT NOT NULL DEFAULT 100 COMMENT '优先级,数字越小越优先',
  `hit_count` INT NOT NULL DEFAULT 0 COMMENT '命中次数(用于排序优化)',
  `enabled` TINYINT NOT NULL DEFAULT 1 COMMENT '是否启用 0-禁用 1-启用',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_ai_rule_keyword` (`keyword`),
  INDEX `idx_ai_rule_account` (`account_set_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI记账规则库';

CREATE TABLE `ai_recognition_feedback` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `original_description` VARCHAR(500) NOT NULL COMMENT '原始业务描述',
  `ai_suggested_debit_code` VARCHAR(20) DEFAULT NULL COMMENT 'AI建议借方科目编码',
  `ai_suggested_credit_code` VARCHAR(20) DEFAULT NULL COMMENT 'AI建议贷方科目编码',
  `actual_debit_code` VARCHAR(20) NOT NULL COMMENT '用户实际选择借方科目编码',
  `actual_credit_code` VARCHAR(20) NOT NULL COMMENT '用户实际选择贷方科目编码',
  `actual_summary` VARCHAR(200) DEFAULT NULL COMMENT '用户实际摘要',
  `accepted` TINYINT NOT NULL DEFAULT 0 COMMENT '是否采纳AI建议 0-未采纳 1-部分采纳 2-完全采纳',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_ai_feedback_desc` (`original_description`),
  INDEX `idx_ai_feedback_account` (`account_set_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='AI识别反馈记录';

INSERT INTO `ai_accounting_rule` (`id`, `account_set_id`, `keyword`, `debit_subject_code`, `debit_subject_name`, `credit_subject_code`, `credit_subject_name`, `voucher_summary`, `priority`, `hit_count`, `enabled`, `deleted`, `version`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, 0, '差旅费', '5602', '管理费用', '1001', '库存现金', '差旅费报销', 100, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(2, 0, '办公费', '5602', '管理费用', '1001', '库存现金', '办公用品采购', 100, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(3, 0, '工资', '5602', '管理费用', '2211', '应付职工薪酬', '计提工资', 90, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(4, 0, '社保', '5602', '管理费用', '2211', '应付职工薪酬', '计提社保', 90, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(5, 0, '公积金', '5602', '管理费用', '2211', '应付职工薪酬', '计提公积金', 90, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(6, 0, '折旧', '5602', '管理费用', '1702', '累计折旧', '计提折旧', 90, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(7, 0, '水电费', '5602', '管理费用', '1002', '银行存款', '支付水电费', 100, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(8, 0, '电话费', '5602', '管理费用', '1002', '银行存款', '支付电话费', 100, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(9, 0, '租金', '5602', '管理费用', '1002', '银行存款', '支付租金', 100, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(10, 0, '采购', '1403', '原材料', '2202', '应付账款', '采购材料', 100, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(11, 0, '销售', '1122', '应收账款', '5001', '主营业务收入', '销售商品', 100, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(12, 0, '进项税', '2221', '应交税费', '2202', '应付账款', '采购进项税', 95, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(13, 0, '销项税', '1122', '应收账款', '2221', '应交税费', '销售销项税', 95, 0, 1, 0, 0, 1, NOW(), 1, NOW());
