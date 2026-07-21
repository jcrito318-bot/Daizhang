-- ============================================
-- V3__add_voucher_template.sql - 凭证模板 + 摘要库表
-- ============================================
-- 新增表:
--   1. voucher_template  - 凭证模板表 (模板编码/名称/分类/分录JSON)
--   2. abstract_library  - 常用摘要库 (智能排序/分类管理)
-- ============================================

CREATE TABLE `voucher_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
  `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `template_category` VARCHAR(50) DEFAULT NULL COMMENT '分类: 工资/折旧/社保/税金/结转/其他',
  `summary` VARCHAR(200) DEFAULT NULL COMMENT '凭证摘要',
  `detail_json` TEXT COMMENT '分录明细 JSON: [{subjectCode, subjectName, debitAmount, creditAmount, summary}]',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_template` (`account_set_id`, `template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='凭证模板表';

CREATE TABLE `abstract_library` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `abstract_text` VARCHAR(200) NOT NULL COMMENT '摘要文本',
  `abstract_category` VARCHAR(50) DEFAULT NULL COMMENT '分类: 工资/折旧/社保/税金/报销/采购/销售/其他',
  `use_count` INT NOT NULL DEFAULT 1 COMMENT '使用次数(用于智能排序)',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  INDEX `idx_account_text` (`account_set_id`, `abstract_text`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='常用摘要库';
