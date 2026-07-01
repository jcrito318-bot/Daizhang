-- ============================================
-- 代账系统数据库初始化脚本
-- MySQL 8.0+
-- ============================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ============================================
-- 1. 系统管理模块
-- ============================================

-- 用户表
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `password` VARCHAR(100) NOT NULL COMMENT '密码',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- 角色表
DROP TABLE IF EXISTS `sys_role`;
CREATE TABLE `sys_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_name` VARCHAR(50) NOT NULL COMMENT '角色名称',
  `role_code` VARCHAR(50) NOT NULL COMMENT '角色编码',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

-- 菜单表
DROP TABLE IF EXISTS `sys_menu`;
CREATE TABLE `sys_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父菜单ID',
  `name` VARCHAR(50) NOT NULL COMMENT '菜单名称',
  `path` VARCHAR(200) DEFAULT NULL COMMENT '路由路径',
  `component` VARCHAR(200) DEFAULT NULL COMMENT '组件路径',
  `icon` VARCHAR(100) DEFAULT NULL COMMENT '图标',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `menu_type` TINYINT NOT NULL COMMENT '菜单类型 1-目录 2-菜单 3-按钮',
  `permission` VARCHAR(100) DEFAULT NULL COMMENT '权限标识',
  `visible` TINYINT NOT NULL DEFAULT 1 COMMENT '是否可见 0-隐藏 1-显示',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单表';

-- 用户角色关联表
DROP TABLE IF EXISTS `sys_user_role`;
CREATE TABLE `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

-- 角色菜单关联表
DROP TABLE IF EXISTS `sys_role_menu`;
CREATE TABLE `sys_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联表';

-- 操作日志表
DROP TABLE IF EXISTS `sys_operation_log`;
CREATE TABLE `sys_operation_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT DEFAULT NULL COMMENT '操作用户ID',
  `username` VARCHAR(50) DEFAULT NULL COMMENT '操作用户名',
  `operation` VARCHAR(200) DEFAULT NULL COMMENT '操作描述',
  `method` VARCHAR(200) DEFAULT NULL COMMENT '请求方法',
  `params` TEXT DEFAULT NULL COMMENT '请求参数',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `status` TINYINT DEFAULT NULL COMMENT '操作状态 0-失败 1-成功',
  `error_msg` TEXT DEFAULT NULL COMMENT '错误信息',
  `cost_time` BIGINT DEFAULT NULL COMMENT '消耗时间(毫秒)',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='操作日志表';

-- ============================================
-- 2. 账套管理模块
-- ============================================

-- 账套表
DROP TABLE IF EXISTS `acc_account_set`;
CREATE TABLE `acc_account_set` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `code` VARCHAR(50) NOT NULL COMMENT '账套编码',
  `name` VARCHAR(200) NOT NULL COMMENT '账套名称',
  `company_name` VARCHAR(500) DEFAULT NULL COMMENT '公司名称',
  `industry_type` VARCHAR(50) DEFAULT NULL COMMENT '行业类型',
  `accounting_standard` VARCHAR(50) NOT NULL DEFAULT '小企业会计准则' COMMENT '会计准则',
  `start_year` INT DEFAULT NULL COMMENT '启用年份',
  `start_month` INT DEFAULT NULL COMMENT '启用月份',
  `currency_code` VARCHAR(10) NOT NULL DEFAULT 'CNY' COMMENT '币种编码',
  `taxpayer_type` VARCHAR(20) DEFAULT NULL COMMENT '纳税人类型 1-小规模 2-一般纳税人',
  `contact_person` VARCHAR(100) DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(50) DEFAULT NULL COMMENT '联系电话',
  `address` VARCHAR(500) DEFAULT NULL COMMENT '地址',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='账套表';

-- 会计期间表
DROP TABLE IF EXISTS `acc_account_period`;
CREATE TABLE `acc_account_period` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `year` INT NOT NULL COMMENT '年',
  `month` INT NOT NULL COMMENT '月',
  `start_date` DATE NOT NULL COMMENT '开始日期',
  `end_date` DATE NOT NULL COMMENT '结束日期',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-未结账 1-已结账',
  `close_by` BIGINT DEFAULT NULL COMMENT '结账人ID',
  `close_time` DATETIME DEFAULT NULL COMMENT '结账时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_year_month` (`account_set_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会计期间表';

-- 科目余额表
DROP TABLE IF EXISTS `acc_account_balance`;
CREATE TABLE `acc_account_balance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `subject_id` BIGINT NOT NULL COMMENT '科目ID',
  `year` INT NOT NULL COMMENT '年',
  `month` INT NOT NULL COMMENT '月',
  `begin_debit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '期初借方余额',
  `begin_credit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '期初贷方余额',
  `period_debit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '本期借方发生额',
  `period_credit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '本期贷方发生额',
  `end_debit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '期末借方余额',
  `end_credit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '期末贷方余额',
  `year_debit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '本年累计借方',
  `year_credit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '本年累计贷方',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_balance` (`account_set_id`, `subject_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='科目余额表';

-- ============================================
-- 3. 科目管理模块
-- ============================================

-- 科目表
DROP TABLE IF EXISTS `acc_subject`;
CREATE TABLE `acc_subject` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `code` VARCHAR(50) NOT NULL COMMENT '科目编码',
  `name` VARCHAR(200) NOT NULL COMMENT '科目名称',
  `category` VARCHAR(50) DEFAULT NULL COMMENT '科目类别 资产/负债/共同/所有者权益/成本/损益',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父科目ID',
  `level` INT NOT NULL DEFAULT 1 COMMENT '科目级次',
  `balance_direction` TINYINT DEFAULT NULL COMMENT '余额方向 1-借 2-贷',
  `is_auxiliary` TINYINT NOT NULL DEFAULT 0 COMMENT '是否辅助核算 0-否 1-是',
  `is_cash` TINYINT NOT NULL DEFAULT 0 COMMENT '是否现金科目 0-否 1-是',
  `is_bank` TINYINT NOT NULL DEFAULT 0 COMMENT '是否银行科目 0-否 1-是',
  `is_current` TINYINT NOT NULL DEFAULT 0 COMMENT '是否损益类 0-否 1-是',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_code` (`account_set_id`, `code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='科目表';

-- ============================================
-- 4. 凭证管理模块
-- ============================================

-- 凭证字表
DROP TABLE IF EXISTS `acc_voucher_word`;
CREATE TABLE `acc_voucher_word` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `name` VARCHAR(50) NOT NULL COMMENT '凭证字名称',
  `code` VARCHAR(20) NOT NULL COMMENT '凭证字编码',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='凭证字表';

-- 凭证表
DROP TABLE IF EXISTS `acc_voucher`;
CREATE TABLE `acc_voucher` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `voucher_word_id` BIGINT NOT NULL COMMENT '凭证字ID',
  `voucher_no` VARCHAR(50) NOT NULL COMMENT '凭证号',
  `voucher_date` DATE NOT NULL COMMENT '凭证日期',
  `year` INT NOT NULL COMMENT '年',
  `month` INT NOT NULL COMMENT '月',
  `total_debit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '借方合计',
  `total_credit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '贷方合计',
  `attachment_count` INT NOT NULL DEFAULT 0 COMMENT '附件数量',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-未审核 1-已审核 2-已过账',
  `audit_by` BIGINT DEFAULT NULL COMMENT '审核人ID',
  `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
  `post_by` BIGINT DEFAULT NULL COMMENT '过账人ID',
  `post_time` DATETIME DEFAULT NULL COMMENT '过账时间',
  `source` TINYINT NOT NULL DEFAULT 0 COMMENT '来源 0-手工录入 1-自动生成',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_year_month_no` (`account_set_id`, `year`, `month`, `voucher_no`),
  KEY `idx_voucher_date` (`voucher_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='凭证表';

-- 凭证明细表
DROP TABLE IF EXISTS `acc_voucher_detail`;
CREATE TABLE `acc_voucher_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `voucher_id` BIGINT NOT NULL COMMENT '凭证ID',
  `line_no` INT NOT NULL COMMENT '行号',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `subject_id` BIGINT NOT NULL COMMENT '科目ID',
  `subject_code` VARCHAR(50) NOT NULL COMMENT '科目编码',
  `subject_name` VARCHAR(200) NOT NULL COMMENT '科目名称',
  `auxiliary_id` BIGINT DEFAULT NULL COMMENT '辅助核算ID',
  `debit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '借方金额',
  `credit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '贷方金额',
  `quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '数量',
  `unit_price` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '单价',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_voucher_id` (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='凭证明细表';

-- ============================================
-- 5. 账簿管理模块
-- ============================================

-- 总账表
DROP TABLE IF EXISTS `acc_general_ledger`;
CREATE TABLE `acc_general_ledger` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `subject_id` BIGINT NOT NULL COMMENT '科目ID',
  `subject_code` VARCHAR(50) NOT NULL COMMENT '科目编码',
  `subject_name` VARCHAR(200) NOT NULL COMMENT '科目名称',
  `year` INT NOT NULL COMMENT '年',
  `month` INT NOT NULL COMMENT '月',
  `begin_debit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '期初借方余额',
  `begin_credit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '期初贷方余额',
  `period_debit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '本期借方发生额',
  `period_credit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '本期贷方发生额',
  `end_debit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '期末借方余额',
  `end_credit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '期末贷方余额',
  `year_debit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '本年累计借方',
  `year_credit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '本年累计贷方',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_general_ledger` (`account_set_id`, `subject_id`, `year`, `month`),
  KEY `idx_subject_code` (`subject_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='总账表';

-- 明细账表
DROP TABLE IF EXISTS `acc_detail_ledger`;
CREATE TABLE `acc_detail_ledger` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `subject_id` BIGINT NOT NULL COMMENT '科目ID',
  `subject_code` VARCHAR(50) NOT NULL COMMENT '科目编码',
  `subject_name` VARCHAR(200) NOT NULL COMMENT '科目名称',
  `year` INT NOT NULL COMMENT '年',
  `month` INT NOT NULL COMMENT '月',
  `voucher_id` BIGINT DEFAULT NULL COMMENT '凭证ID',
  `voucher_no` VARCHAR(50) DEFAULT NULL COMMENT '凭证号',
  `voucher_date` DATE DEFAULT NULL COMMENT '凭证日期',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `opposite_subject_name` VARCHAR(200) DEFAULT NULL COMMENT '对方科目名称',
  `debit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '借方金额',
  `credit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '贷方金额',
  `balance_direction` TINYINT DEFAULT NULL COMMENT '余额方向 1-借 2-贷',
  `balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '余额',
  `auxiliary_id` BIGINT DEFAULT NULL COMMENT '辅助核算ID',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_subject_year_month` (`account_set_id`, `subject_id`, `year`, `month`),
  KEY `idx_voucher_id` (`voucher_id`),
  KEY `idx_voucher_date` (`voucher_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='明细账表';

-- ============================================
-- 6. 期末处理模块
-- ============================================

-- 结账记录表
DROP TABLE IF EXISTS `acc_period_close`;
CREATE TABLE `acc_period_close` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `year` INT NOT NULL COMMENT '年',
  `month` INT NOT NULL COMMENT '月',
  `close_status` TINYINT NOT NULL DEFAULT 0 COMMENT '结账状态 0-未结账 1-已结账 2-已反结账',
  `close_by` BIGINT DEFAULT NULL COMMENT '结账人ID',
  `close_time` DATETIME DEFAULT NULL COMMENT '结账时间',
  `unclose_by` BIGINT DEFAULT NULL COMMENT '反结账人ID',
  `unclose_time` DATETIME DEFAULT NULL COMMENT '反结账时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_year_month` (`account_set_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='结账记录表';

-- ============================================
-- 7. 财务报表模块
-- ============================================

-- 资产负债表
DROP TABLE IF EXISTS `rpt_balance_sheet`;
CREATE TABLE `rpt_balance_sheet` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `year` INT NOT NULL COMMENT '年',
  `month` INT NOT NULL COMMENT '月',
  `item_name` VARCHAR(100) NOT NULL COMMENT '项目名称',
  `item_code` VARCHAR(50) NOT NULL COMMENT '项目编码',
  `row_no` INT NOT NULL COMMENT '行号',
  `begin_balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '年初余额',
  `end_balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '期末余额',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_balance_sheet` (`account_set_id`, `year`, `month`, `item_code`),
  KEY `idx_account_set_year_month` (`account_set_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产负债表';

-- 利润表
DROP TABLE IF EXISTS `rpt_income_statement`;
CREATE TABLE `rpt_income_statement` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `year` INT NOT NULL COMMENT '年',
  `month` INT NOT NULL COMMENT '月',
  `item_name` VARCHAR(100) NOT NULL COMMENT '项目名称',
  `item_code` VARCHAR(50) NOT NULL COMMENT '项目编码',
  `row_no` INT NOT NULL COMMENT '行号',
  `current_period` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '本期金额',
  `year_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '本年累计金额',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_income_statement` (`account_set_id`, `year`, `month`, `item_code`),
  KEY `idx_account_set_year_month` (`account_set_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='利润表';

-- ============================================
-- 8. 票据管理模块
-- ============================================

-- 票据表
DROP TABLE IF EXISTS `doc_document`;
CREATE TABLE `doc_document` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `document_no` VARCHAR(100) NOT NULL COMMENT '票据编号',
  `document_type` TINYINT NOT NULL COMMENT '票据类型 1-发票 2-银行回单 3-费用单据 4-其他',
  `document_date` DATE NOT NULL COMMENT '票据日期',
  `amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '金额',
  `tax_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '税额',
  `total_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '价税合计',
  `seller_name` VARCHAR(200) DEFAULT NULL COMMENT '销方名称',
  `buyer_name` VARCHAR(200) DEFAULT NULL COMMENT '购方名称',
  `invoice_code` VARCHAR(50) DEFAULT NULL COMMENT '发票代码',
  `invoice_number` VARCHAR(50) DEFAULT NULL COMMENT '发票号码',
  `ocr_content` TEXT DEFAULT NULL COMMENT 'OCR识别内容',
  `file_url` VARCHAR(500) DEFAULT NULL COMMENT '文件地址',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待处理 1-已关联凭证 2-已完成',
  `voucher_id` BIGINT DEFAULT NULL COMMENT '关联凭证ID',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_document_no` (`account_set_id`, `document_no`),
  KEY `idx_document_type` (`document_type`),
  KEY `idx_document_date` (`document_date`),
  KEY `idx_voucher_id` (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='票据表';

-- ============================================
-- 9. 税务管理模块
-- ============================================

-- 税务申报表
DROP TABLE IF EXISTS `tax_declaration`;
CREATE TABLE `tax_declaration` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `year` INT NOT NULL COMMENT '年',
  `month` INT NOT NULL COMMENT '月',
  `tax_type` VARCHAR(50) NOT NULL COMMENT '税种：增值税/企业所得税/个人所得税等',
  `taxable_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '应纳税所得额',
  `tax_rate` DECIMAL(10,4) DEFAULT NULL COMMENT '税率',
  `tax_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '应纳税额',
  `declared_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '已申报金额',
  `actual_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '实际缴纳金额',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-未申报 1-已申报 2-已缴纳',
  `declaration_date` DATE DEFAULT NULL COMMENT '申报日期',
  `payment_date` DATE DEFAULT NULL COMMENT '缴纳日期',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_account_set_year_month` (`account_set_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='税务申报表';

-- 税务计算表
DROP TABLE IF EXISTS `tax_calculation`;
CREATE TABLE `tax_calculation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `year` INT NOT NULL COMMENT '年',
  `month` INT NOT NULL COMMENT '月',
  `tax_type` VARCHAR(50) NOT NULL COMMENT '税种：增值税/企业所得税/个人所得税等',
  `calculation_item` VARCHAR(200) NOT NULL COMMENT '计算项目',
  `amount` DECIMAL(18,2) DEFAULT NULL COMMENT '金额',
  `rate` DECIMAL(10,4) DEFAULT NULL COMMENT '税率',
  `tax_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '税额',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_account_set_year_month` (`account_set_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='税务计算表';

-- ============================================
-- 10. 银行对账模块
-- ============================================

-- 银行流水表
DROP TABLE IF EXISTS `bank_transaction`;
CREATE TABLE `bank_transaction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `bank_account` VARCHAR(50) NOT NULL COMMENT '银行账号',
  `transaction_date` DATE NOT NULL COMMENT '交易日期',
  `transaction_type` TINYINT NOT NULL COMMENT '交易类型 1-收入 2-支出',
  `amount` DECIMAL(18,2) NOT NULL COMMENT '交易金额',
  `balance` DECIMAL(18,2) DEFAULT NULL COMMENT '账户余额',
  `counterparty` VARCHAR(200) DEFAULT NULL COMMENT '对方单位',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `transaction_no` VARCHAR(100) DEFAULT NULL COMMENT '交易流水号',
  `matched_status` TINYINT NOT NULL DEFAULT 0 COMMENT '匹配状态 0-未匹配 1-已匹配',
  `voucher_id` BIGINT DEFAULT NULL COMMENT '关联凭证ID',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_account_set_bank` (`account_set_id`, `bank_account`),
  KEY `idx_transaction_date` (`transaction_date`),
  KEY `idx_matched_status` (`matched_status`),
  KEY `idx_transaction_no` (`transaction_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='银行流水表';

-- 银行对账结果表
DROP TABLE IF EXISTS `bank_reconciliation`;
CREATE TABLE `bank_reconciliation` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `bank_account` VARCHAR(50) NOT NULL COMMENT '银行账号',
  `year` INT NOT NULL COMMENT '年度',
  `month` INT NOT NULL COMMENT '月份',
  `bank_balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '银行余额',
  `book_balance` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '账簿余额',
  `unreconciled_items` INT NOT NULL DEFAULT 0 COMMENT '未达账项数量',
  `reconciled_date` DATE DEFAULT NULL COMMENT '对账日期',
  `reconciled_by` BIGINT DEFAULT NULL COMMENT '对账人ID',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '对账状态 0-未对账 1-已对账',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_bank_year_month` (`account_set_id`, `bank_account`, `year`, `month`),
  KEY `idx_reconciled_date` (`reconciled_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='银行对账结果表';

-- ============================================
-- 11. 薪资管理模块
-- ============================================

-- 员工表
DROP TABLE IF EXISTS `sal_employee`;
CREATE TABLE `sal_employee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `employee_code` VARCHAR(50) NOT NULL COMMENT '员工编号',
  `employee_name` VARCHAR(50) NOT NULL COMMENT '员工姓名',
  `base_salary` DECIMAL(18,2) DEFAULT 0.00 COMMENT '基本工资',
  `department` VARCHAR(100) DEFAULT NULL COMMENT '部门',
  `position` VARCHAR(100) DEFAULT NULL COMMENT '职位',
  `id_card` VARCHAR(20) DEFAULT NULL COMMENT '身份证号',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `entry_date` DATE DEFAULT NULL COMMENT '入职日期',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-离职 1-在职',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_employee_code` (`account_set_id`, `employee_code`),
  KEY `idx_account_set_id` (`account_set_id`),
  KEY `idx_employee_name` (`employee_name`),
  KEY `idx_department` (`department`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工表';

-- 薪资项目表
DROP TABLE IF EXISTS `sal_salary_item`;
CREATE TABLE `sal_salary_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `item_name` VARCHAR(100) NOT NULL COMMENT '项目名称',
  `item_code` VARCHAR(50) NOT NULL COMMENT '项目编码',
  `item_type` VARCHAR(20) NOT NULL COMMENT '项目类型 FIXED-固定 FLOAT-浮动 DEDUCT-扣款',
  `calculation_method` VARCHAR(500) DEFAULT NULL COMMENT '计算方法',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_item_code` (`account_set_id`, `item_code`),
  KEY `idx_account_set_id` (`account_set_id`),
  KEY `idx_item_type` (`item_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='薪资项目表';

-- 薪资表
DROP TABLE IF EXISTS `sal_salary_sheet`;
CREATE TABLE `sal_salary_sheet` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `year` INT NOT NULL COMMENT '年度',
  `month` INT NOT NULL COMMENT '月份',
  `employee_id` BIGINT NOT NULL COMMENT '员工ID',
  `employee_name` VARCHAR(50) NOT NULL COMMENT '员工姓名',
  `base_salary` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '基本工资',
  `allowance` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '津贴补贴',
  `bonus` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '奖金',
  `deduction` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '扣款',
  `social_security` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '社保',
  `housing_fund` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '公积金',
  `taxable_income` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '应纳税所得额',
  `income_tax` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '个人所得税',
  `net_salary` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '实发工资',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-草稿 1-已确认 2-已发放',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_year_month_employee` (`account_set_id`, `year`, `month`, `employee_id`),
  KEY `idx_account_set_year_month` (`account_set_id`, `year`, `month`),
  KEY `idx_employee_id` (`employee_id`),
  KEY `idx_employee_name` (`employee_name`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='薪资表';

-- ============================================
-- 12. 固定资产模块
-- ============================================

-- 资产分类表
DROP TABLE IF EXISTS `asset_category`;
CREATE TABLE `asset_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `category_code` VARCHAR(50) NOT NULL COMMENT '分类编码',
  `category_name` VARCHAR(100) NOT NULL COMMENT '分类名称',
  `depreciation_method` VARCHAR(50) DEFAULT NULL COMMENT '折旧方法：直线法/工作量法/双倍余额递减法',
  `useful_life` INT DEFAULT NULL COMMENT '使用年限（月）',
  `residual_rate` DECIMAL(10,4) DEFAULT NULL COMMENT '残值率（%）',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父分类ID',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_category_code` (`account_set_id`, `category_code`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产分类表';

-- 固定资产表
DROP TABLE IF EXISTS `asset_fixed`;
CREATE TABLE `asset_fixed` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `asset_code` VARCHAR(50) NOT NULL COMMENT '资产编码',
  `asset_name` VARCHAR(200) NOT NULL COMMENT '资产名称',
  `category_id` BIGINT NOT NULL COMMENT '分类ID',
  `category_name` VARCHAR(100) DEFAULT NULL COMMENT '分类名称',
  `purchase_date` DATE NOT NULL COMMENT '购入日期',
  `purchase_amount` DECIMAL(18,2) NOT NULL COMMENT '购入金额',
  `depreciation_method` VARCHAR(50) NOT NULL COMMENT '折旧方法：直线法/工作量法/双倍余额递减法',
  `useful_life` INT NOT NULL COMMENT '使用年限（月）',
  `residual_value` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '残值',
  `monthly_depreciation` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '月折旧额',
  `accumulated_deprecation` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计折旧',
  `net_value` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '净值',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-在用 1-闲置 2-报废',
  `department` VARCHAR(100) DEFAULT NULL COMMENT '使用部门',
  `keeper` VARCHAR(100) DEFAULT NULL COMMENT '保管人',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_asset_code` (`account_set_id`, `asset_code`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='固定资产表';

-- 折旧记录表
DROP TABLE IF EXISTS `asset_depreciation_record`;
CREATE TABLE `asset_depreciation_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `asset_id` BIGINT NOT NULL COMMENT '资产ID',
  `asset_code` VARCHAR(50) NOT NULL COMMENT '资产编码',
  `asset_name` VARCHAR(200) NOT NULL COMMENT '资产名称',
  `year` INT NOT NULL COMMENT '年度',
  `month` INT NOT NULL COMMENT '月份',
  `depreciation_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '折旧金额',
  `accumulated_depreciation` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '累计折旧',
  `net_value` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '净值',
  `voucher_id` BIGINT DEFAULT NULL COMMENT '凭证ID',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_year_month` (`account_set_id`, `asset_id`, `year`, `month`),
  KEY `idx_asset_id` (`asset_id`),
  KEY `idx_voucher_id` (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='折旧记录表';

-- ============================================
-- 13. 客户管理模块
-- ============================================

-- 客户表
DROP TABLE IF EXISTS `cst_customer`;
CREATE TABLE `cst_customer` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `customer_code` VARCHAR(50) NOT NULL COMMENT '客户编码',
  `customer_name` VARCHAR(200) NOT NULL COMMENT '客户名称',
  `customer_type` VARCHAR(20) DEFAULT NULL COMMENT '客户类型 企业/个人',
  `industry` VARCHAR(100) DEFAULT NULL COMMENT '行业',
  `scale` VARCHAR(50) DEFAULT NULL COMMENT '规模',
  `taxpayer_type` VARCHAR(50) DEFAULT NULL COMMENT '纳税人类型 一般纳税人/小规模纳税人',
  `contact_person` VARCHAR(100) DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(50) DEFAULT NULL COMMENT '联系电话',
  `email` VARCHAR(100) DEFAULT NULL COMMENT '邮箱',
  `address` VARCHAR(500) DEFAULT NULL COMMENT '地址',
  `tax_no` VARCHAR(50) DEFAULT NULL COMMENT '税号',
  `bank_name` VARCHAR(200) DEFAULT NULL COMMENT '开户银行',
  `bank_account` VARCHAR(50) DEFAULT NULL COMMENT '银行账号',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-启用',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_customer_code` (`account_set_id`, `customer_code`),
  KEY `idx_customer_name` (`customer_name`),
  KEY `idx_customer_type` (`customer_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户表';

-- 服务合同表
DROP TABLE IF EXISTS `cst_service_contract`;
CREATE TABLE `cst_service_contract` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `contract_no` VARCHAR(50) NOT NULL COMMENT '合同编号',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `contract_name` VARCHAR(200) NOT NULL COMMENT '合同名称',
  `contract_type` VARCHAR(50) DEFAULT NULL COMMENT '合同类型',
  `start_date` DATE DEFAULT NULL COMMENT '开始日期',
  `end_date` DATE DEFAULT NULL COMMENT '结束日期',
  `service_content` TEXT DEFAULT NULL COMMENT '服务内容',
  `amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '合同金额',
  `payment_method` VARCHAR(50) DEFAULT NULL COMMENT '付款方式',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-草稿 1-执行中 2-已完成 3-已终止',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_contract_no` (`account_set_id`, `contract_no`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='服务合同表';

-- 收款记录表
DROP TABLE IF EXISTS `cst_payment_record`;
CREATE TABLE `cst_payment_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `contract_id` BIGINT NOT NULL COMMENT '合同ID',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `payment_date` DATE NOT NULL COMMENT '收款日期',
  `amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '收款金额',
  `payment_method` VARCHAR(50) DEFAULT NULL COMMENT '收款方式',
  `payment_type` VARCHAR(50) DEFAULT NULL COMMENT '收款类型',
  `voucher_no` VARCHAR(50) DEFAULT NULL COMMENT '凭证号',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_customer_id` (`customer_id`),
  KEY `idx_payment_date` (`payment_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='收款记录表';

-- ============================================
-- 初始化数据
-- ============================================

-- 默认管理员用户 (密码: admin123)
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `email`, `status`, `deleted`, `version`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, 'admin', '$2a$10$7JB720yubVSZvUI0rEqW/.VqGOi8.4hJEjOHq.KEj.wF/UGX2O6Ze', '系统管理员', '13800138000', 'admin@daizhang.com', 1, 0, 0, 1, NOW(), 1, NOW());

-- 默认角色
INSERT INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `status`, `deleted`, `version`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, '管理员', 'ADMIN', '系统管理员，拥有所有权限', 1, 0, 0, 1, NOW(), 1, NOW()),
(2, '代账主管', 'MANAGER', '代账主管，管理代账业务', 1, 0, 0, 1, NOW(), 1, NOW()),
(3, '会计', 'ACCOUNTANT', '会计人员，负责账务处理', 1, 0, 0, 1, NOW(), 1, NOW()),
(4, '出纳', 'CASHIER', '出纳人员，负责资金管理', 1, 0, 0, 1, NOW(), 1, NOW());

-- 默认菜单
INSERT INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `icon`, `sort_order`, `menu_type`, `permission`, `visible`, `status`, `deleted`, `version`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
-- 一级菜单
(1, 0, '仪表盘', '/dashboard', 'dashboard/index', 'dashboard', 1, 2, 'dashboard:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(2, 0, '账套管理', '/account-set', 'account-set/index', 'account', 2, 2, 'account-set:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(3, 0, '科目管理', '/subject', 'subject/index', 'subject', 3, 2, 'subject:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(4, 0, '凭证管理', '/voucher', 'voucher/index', 'voucher', 4, 2, 'voucher:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(5, 0, '账簿查询', '/book', '', 'book', 5, 1, '', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(6, 0, '期末处理', '/period', '', 'period', 6, 1, '', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(7, 0, '财务报表', '/report', '', 'report', 7, 1, '', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(8, 0, '票据管理', '/document', 'document/index', 'document', 8, 2, 'document:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(9, 0, '税务管理', '/tax', '', 'tax', 9, 1, '', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(10, 0, '银行对账', '/bank', '', 'bank', 10, 1, '', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(11, 0, '薪资管理', '/salary', '', 'salary', 11, 1, '', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(12, 0, '固定资产', '/asset', '', 'asset', 12, 1, '', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(13, 0, '客户管理', '/customer', '', 'customer', 13, 1, '', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(14, 0, '系统管理', '/system', '', 'system', 14, 1, '', 1, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 账簿查询子菜单
(51, 5, '明细账', '/book/detail', 'book/detail', 'detail', 1, 2, 'book:detail:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(52, 5, '总账', '/book/total', 'book/total', 'total', 2, 2, 'book:total:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(53, 5, '科目余额表', '/book/balance', 'book/balance', 'balance', 3, 2, 'book:balance:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(54, 5, '日记账', '/book/journal', 'book/journal', 'journal', 4, 2, 'book:journal:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 期末处理子菜单
(61, 6, '结转', '/period/carry-forward', 'period/carry-forward', 'carry', 1, 2, 'period:carry:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(62, 6, '结账', '/period/close', 'period/close', 'close', 2, 2, 'period:close:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 财务报表子菜单
(71, 7, '资产负债表', '/report/balance-sheet', 'report/balance-sheet', 'balance-sheet', 1, 2, 'report:balance:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(72, 7, '利润表', '/report/income', 'report/income', 'income', 2, 2, 'report:income:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(73, 7, '现金流量表', '/report/cash-flow', 'report/cash-flow', 'cash-flow', 3, 2, 'report:cashflow:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 税务管理子菜单
(91, 9, '税务申报', '/tax/declaration', 'tax/declaration', 'declaration', 1, 2, 'tax:declaration:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(92, 9, '税务计算', '/tax/calculation', 'tax/calculation', 'calculation', 2, 2, 'tax:calculation:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 银行对账子菜单
(101, 10, '银行流水', '/bank/transaction', 'bank/transaction', 'transaction', 1, 2, 'bank:transaction:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(102, 10, '银行对账', '/bank/reconciliation', 'bank/reconciliation', 'reconciliation', 2, 2, 'bank:reconciliation:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 薪资管理子菜单
(111, 11, '员工管理', '/salary/employee', 'salary/employee', 'employee', 1, 2, 'salary:employee:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(112, 11, '薪资项目', '/salary/item', 'salary/item', 'item', 2, 2, 'salary:item:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(113, 11, '薪资表', '/salary/sheet', 'salary/sheet', 'sheet', 3, 2, 'salary:sheet:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 固定资产子菜单
(121, 12, '资产分类', '/asset/category', 'asset/category', 'category', 1, 2, 'asset:category:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(122, 12, '固定资产', '/asset/fixed', 'asset/fixed', 'fixed', 2, 2, 'asset:fixed:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(123, 12, '折旧记录', '/asset/depreciation', 'asset/depreciation', 'depreciation', 3, 2, 'asset:depreciation:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 客户管理子菜单
(131, 13, '客户列表', '/customer/list', 'customer/list', 'list', 1, 2, 'customer:list:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(132, 13, '服务合同', '/customer/contract', 'customer/contract', 'contract', 2, 2, 'customer:contract:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(133, 13, '收款记录', '/customer/payment', 'customer/payment', 'payment', 3, 2, 'customer:payment:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 系统管理子菜单
(141, 14, '用户管理', '/system/user', 'system/user', 'user', 1, 2, 'system:user:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(142, 14, '角色管理', '/system/role', 'system/role', 'role', 2, 2, 'system:role:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(143, 14, '菜单管理', '/system/menu', 'system/menu', 'menu', 3, 2, 'system:menu:view', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(144, 14, '操作日志', '/system/log', 'system/log', 'log', 4, 2, 'system:log:view', 1, 1, 0, 0, 1, NOW(), 1, NOW());

-- 管理员用户角色关联
INSERT INTO `sys_user_role` (`id`, `user_id`, `role_id`) VALUES
(1, 1, 1);

-- 管理员角色菜单关联 (所有菜单)
INSERT INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) VALUES
(1, 1, 1), (2, 1, 2), (3, 1, 3), (4, 1, 4), (5, 1, 5), (6, 1, 6), (7, 1, 7), (8, 1, 8),
(9, 1, 9), (10, 1, 10), (11, 1, 11), (12, 1, 12), (13, 1, 13), (14, 1, 14),
(15, 1, 51), (16, 1, 52), (17, 1, 53), (18, 1, 54),
(19, 1, 61), (20, 1, 62),
(21, 1, 71), (22, 1, 72), (23, 1, 73),
(24, 1, 91), (25, 1, 92),
(26, 1, 101), (27, 1, 102),
(28, 1, 111), (29, 1, 112), (30, 1, 113),
(31, 1, 121), (32, 1, 122), (33, 1, 123),
(34, 1, 131), (35, 1, 132), (36, 1, 133),
(37, 1, 141), (38, 1, 142), (39, 1, 143), (40, 1, 144);

-- 默认凭证字 (账套ID=0表示系统默认，创建账套时复制)
INSERT INTO `acc_voucher_word` (`id`, `account_set_id`, `name`, `code`, `sort_order`, `status`, `deleted`, `version`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, 0, '收', '收', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(2, 0, '付', '付', 2, 1, 0, 0, 1, NOW(), 1, NOW()),
(3, 0, '转', '转', 3, 1, 0, 0, 1, NOW(), 1, NOW()),
(4, 0, '记', '记', 4, 1, 0, 0, 1, NOW(), 1, NOW());

-- ============================================
-- 标准科目模板 (account_set_id=0 表示系统默认模板)
-- ============================================

-- 资产类科目
INSERT INTO `acc_subject` (`id`, `account_set_id`, `code`, `name`, `category`, `parent_id`, `level`, `balance_direction`, `is_auxiliary`, `is_cash`, `is_bank`, `is_current`, `status`, `deleted`, `version`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
-- 一级科目
(1, 0, '1001', '库存现金', '资产', 0, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(2, 0, '1002', '银行存款', '资产', 0, 1, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(3, 0, '1012', '其他货币资金', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(4, 0, '1101', '短期投资', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(5, 0, '1121', '应收票据', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(6, 0, '1122', '应收账款', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(7, 0, '1123', '预付账款', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(8, 0, '1131', '应收股利', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(9, 0, '1132', '应收利息', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(10, 0, '1221', '其他应收款', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(11, 0, '1401', '材料采购', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(12, 0, '1403', '原材料', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(13, 0, '1405', '库存商品', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(14, 0, '1501', '持有至到期投资', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(15, 0, '1601', '长期股权投资', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(16, 0, '1604', '投资性房地产', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(17, 0, '1701', '固定资产', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(18, 0, '1702', '累计折旧', '资产', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(19, 0, '1703', '在建工程', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(20, 0, '1704', '工程物资', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(21, 0, '1801', '无形资产', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(22, 0, '1802', '累计摊销', '资产', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(23, 0, '1901', '长期待摊费用', '资产', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 负债类科目
(24, 0, '2001', '短期借款', '负债', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(25, 0, '2201', '应付票据', '负债', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(26, 0, '2202', '应付账款', '负债', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(27, 0, '2203', '预收账款', '负债', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(28, 0, '2211', '应付职工薪酬', '负债', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(29, 0, '2221', '应交税费', '负债', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(30, 0, '2231', '应付利息', '负债', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(31, 0, '2232', '应付利润', '负债', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(32, 0, '2241', '其他应付款', '负债', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(33, 0, '2501', '长期借款', '负债', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(34, 0, '2502', '长期应付款', '负债', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 所有者权益类科目
(35, 0, '3001', '实收资本', '所有者权益', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(36, 0, '3002', '资本公积', '所有者权益', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(37, 0, '3101', '盈余公积', '所有者权益', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(38, 0, '3103', '本年利润', '所有者权益', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(39, 0, '3104', '利润分配', '所有者权益', 0, 1, 2, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 成本类科目
(40, 0, '4001', '生产成本', '成本', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(41, 0, '4101', '制造费用', '成本', 0, 1, 1, 0, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 损益类科目 - 收入
(42, 0, '5001', '主营业务收入', '损益', 0, 1, 2, 0, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(43, 0, '5051', '其他业务收入', '损益', 0, 1, 2, 0, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(44, 0, '5301', '营业外收入', '损益', 0, 1, 2, 0, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(45, 0, '5111', '投资收益', '损益', 0, 1, 2, 0, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 损益类科目 - 费用
(46, 0, '5401', '主营业务成本', '损益', 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(47, 0, '5402', '其他业务成本', '损益', 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(48, 0, '5403', '营业税金及附加', '损益', 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(49, 0, '5601', '销售费用', '损益', 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(50, 0, '5602', '管理费用', '损益', 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(51, 0, '5603', '财务费用', '损益', 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(52, 0, '5711', '营业外支出', '损益', 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(53, 0, '5801', '所得税费用', '损益', 0, 1, 1, 0, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW());

SET FOREIGN_KEY_CHECKS = 1;
