-- ============================================
-- 代账系统数据库初始化脚本 (H2 兼容版, MODE=MySQL)
-- 基于 init.sql 转换
-- ============================================

-- ============================================
-- 1. 系统管理模块
-- ============================================

-- 用户表
CREATE TABLE IF NOT EXISTS `sys_user` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户表';

-- 角色表
CREATE TABLE IF NOT EXISTS `sys_role` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_code` (`role_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色表';

-- 菜单表
CREATE TABLE IF NOT EXISTS `sys_menu` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='菜单表';

-- 用户角色关联表
CREATE TABLE IF NOT EXISTS `sys_user_role` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_role` (`user_id`, `role_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户角色关联表';

-- 角色菜单关联表
CREATE TABLE IF NOT EXISTS `sys_role_menu` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `role_id` BIGINT NOT NULL COMMENT '角色ID',
  `menu_id` BIGINT NOT NULL COMMENT '菜单ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_menu` (`role_id`, `menu_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='角色菜单关联表';

-- 用户账套关联表(数据级授权:IDOR越权治理基础)
CREATE TABLE IF NOT EXISTS `sys_user_account_set` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `role_type` VARCHAR(20) NOT NULL DEFAULT 'OWNER' COMMENT '关系类型 OWNER-所有者 ACCOUNTANT-记账员 VIEWER-查看者',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_account_set` (`user_id`, `account_set_id`),
  KEY `idx_account_set_users` (`account_set_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='用户账套关联表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `sys_operation_log` (
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
CREATE TABLE IF NOT EXISTS `acc_account_set` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_code` (`code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='账套表';

-- 会计期间表
CREATE TABLE IF NOT EXISTS `acc_account_period` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_year_month` (`account_set_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='会计期间表';

-- 科目余额表
CREATE TABLE IF NOT EXISTS `acc_account_balance` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_balance` (`account_set_id`, `subject_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='科目余额表';

-- ============================================
-- 3. 科目管理模块
-- ============================================

-- 科目表
CREATE TABLE IF NOT EXISTS `acc_subject` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_code` (`account_set_id`, `code`),
  KEY `idx_parent_id` (`parent_id`),
  KEY `idx_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='科目表';

-- ============================================
-- 4. 凭证管理模块
-- ============================================

-- 凭证字表
CREATE TABLE IF NOT EXISTS `acc_voucher_word` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='凭证字表';

-- 凭证表
CREATE TABLE IF NOT EXISTS `acc_voucher` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `voucher_word_id` BIGINT DEFAULT NULL COMMENT '凭证字ID',
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
  `draft_status` TINYINT NOT NULL DEFAULT 0 COMMENT '草稿状态 0-正常 1-草稿',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_year_month_no` (`account_set_id`, `year`, `month`, `voucher_no`, `deleted`),
  KEY `idx_voucher_date` (`voucher_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='凭证表';

-- 凭证明细表
CREATE TABLE IF NOT EXISTS `acc_voucher_detail` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_voucher_id` (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='凭证明细表';

-- ============================================
-- 5. 账簿管理模块
-- ============================================

-- 总账表
CREATE TABLE IF NOT EXISTS `acc_general_ledger` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_general_ledger` (`account_set_id`, `subject_id`, `year`, `month`),
  KEY `idx_subject_code` (`subject_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='总账表';

-- 明细账表
CREATE TABLE IF NOT EXISTS `acc_detail_ledger` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_detail_subject_year_month` (`account_set_id`, `subject_id`, `year`, `month`),
  KEY `idx_detail_voucher_id` (`voucher_id`),
  KEY `idx_detail_voucher_date` (`voucher_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='明细账表';

-- ============================================
-- 6. 期末处理模块
-- ============================================

-- 结账记录表
CREATE TABLE IF NOT EXISTS `acc_period_close` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_period_close_year_month` (`account_set_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='结账记录表';

-- ============================================
-- 7. 财务报表模块
-- ============================================

-- 资产负债表
CREATE TABLE IF NOT EXISTS `rpt_balance_sheet` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_balance_sheet` (`account_set_id`, `year`, `month`, `item_code`),
  KEY `idx_account_set_year_month` (`account_set_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产负债表';

-- 利润表
CREATE TABLE IF NOT EXISTS `rpt_income_statement` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_income_statement` (`account_set_id`, `year`, `month`, `item_code`),
  KEY `idx_income_year_month` (`account_set_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='利润表';

-- ============================================
-- 8. 票据管理模块
-- ============================================

-- 票据表
CREATE TABLE IF NOT EXISTS `doc_document` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_document_no` (`account_set_id`, `document_no`),
  -- B-012: 防止并发重复发票入账(invoice_code/number 为 NULL 时 MySQL/H2 唯一索引不约束,不影响无发票号票据)
  UNIQUE KEY `uk_document_invoice` (`account_set_id`, `invoice_code`, `invoice_number`),
  KEY `idx_document_type` (`document_type`),
  KEY `idx_document_date` (`document_date`),
  KEY `idx_document_voucher_id` (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='票据表';

-- ============================================
-- 9. 税务管理模块
-- ============================================

-- 税务申报表
CREATE TABLE IF NOT EXISTS `tax_declaration` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tax_decl_year_month` (`account_set_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='税务申报表';

-- 税务计算表
CREATE TABLE IF NOT EXISTS `tax_calculation` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_tax_calc_year_month` (`account_set_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='税务计算表';

-- ============================================
-- 10. 银行对账模块
-- ============================================

-- 银行流水表
CREATE TABLE IF NOT EXISTS `bank_transaction` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_account_set_bank` (`account_set_id`, `bank_account`),
  KEY `idx_transaction_date` (`transaction_date`),
  KEY `idx_matched_status` (`matched_status`),
  KEY `idx_transaction_no` (`transaction_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='银行流水表';

-- 银行对账结果表
CREATE TABLE IF NOT EXISTS `bank_reconciliation` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_bank_year_month` (`account_set_id`, `bank_account`, `year`, `month`),
  KEY `idx_reconciled_date` (`reconciled_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='银行对账结果表';

-- 银行匹配历史模式表(智能对账增强:记录同一交易对方历史匹配的金额范围与对应科目,用于智能匹配加分)
CREATE TABLE IF NOT EXISTS `bank_match_history` (
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

-- 银行账户主数据表
CREATE TABLE IF NOT EXISTS `bank_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `account_name` VARCHAR(100) NOT NULL COMMENT '账户名称',
  `account_number` VARCHAR(50) NOT NULL COMMENT '银行账号',
  `bank_name` VARCHAR(100) DEFAULT NULL COMMENT '开户行名称',
  `branch_name` VARCHAR(100) DEFAULT NULL COMMENT '开户行支行',
  `account_type` VARCHAR(20) DEFAULT 'CHECKING' COMMENT '账户类型 CHECKING-活期 DEPOSIT-定期 OTHER-其他',
  `currency` VARCHAR(10) DEFAULT 'CNY' COMMENT '币种',
  `subject_id` BIGINT DEFAULT NULL COMMENT '关联科目ID',
  `beginning_balance` DECIMAL(18,2) DEFAULT 0 COMMENT '期初余额',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-正常',
  `open_date` DATE DEFAULT NULL COMMENT '开户日期',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_account_number` (`account_set_id`, `account_number`),
  KEY `idx_bank_account_account_set_id` (`account_set_id`),
  KEY `idx_bank_account_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='银行账户主数据表';

-- ============================================
-- 11. 薪资管理模块
-- ============================================

-- 员工表
CREATE TABLE IF NOT EXISTS `sal_employee` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `employee_code` VARCHAR(50) NOT NULL COMMENT '员工编号',
  `employee_name` VARCHAR(50) NOT NULL COMMENT '员工姓名',
  `base_salary` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '基本工资',
  `department` VARCHAR(100) DEFAULT NULL COMMENT '部门',
  `job_position` VARCHAR(100) DEFAULT NULL COMMENT '职位',
  `id_card` VARCHAR(20) DEFAULT NULL COMMENT '身份证号',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `bank_name` VARCHAR(200) DEFAULT NULL COMMENT '开户银行',
  `bank_account` VARCHAR(50) DEFAULT NULL COMMENT '银行账号',
  `entry_date` DATE DEFAULT NULL COMMENT '入职日期',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-离职 1-在职',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_employee_code` (`account_set_id`, `employee_code`),
  KEY `idx_sal_employee_account_set_id` (`account_set_id`),
  KEY `idx_employee_name` (`employee_name`),
  KEY `idx_department` (`department`),
  KEY `idx_sal_employee_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工表';

-- 薪资项目表
CREATE TABLE IF NOT EXISTS `sal_salary_item` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_item_code` (`account_set_id`, `item_code`),
  KEY `idx_salary_item_account_set_id` (`account_set_id`),
  KEY `idx_item_type` (`item_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='薪资项目表';

-- 薪资表
CREATE TABLE IF NOT EXISTS `sal_salary_sheet` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_year_month_employee` (`account_set_id`, `year`, `month`, `employee_id`),
  KEY `idx_salary_sheet_year_month` (`account_set_id`, `year`, `month`),
  KEY `idx_sal_salary_sheet_employee_id` (`employee_id`),
  KEY `idx_salary_sheet_employee_name` (`employee_name`),
  KEY `idx_salary_sheet_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='薪资表';

-- ============================================
-- 12. 固定资产模块
-- ============================================

-- 资产分类表
CREATE TABLE IF NOT EXISTS `asset_category` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_category_code` (`account_set_id`, `category_code`),
  KEY `idx_asset_category_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产分类表';

-- 固定资产表
CREATE TABLE IF NOT EXISTS `asset_fixed` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_asset_code` (`account_set_id`, `asset_code`),
  KEY `idx_category_id` (`category_id`),
  KEY `idx_asset_fixed_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='固定资产表';

-- 折旧记录表
CREATE TABLE IF NOT EXISTS `asset_depreciation_record` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_asset_year_month` (`account_set_id`, `asset_id`, `year`, `month`),
  KEY `idx_asset_id` (`asset_id`),
  KEY `idx_asset_dep_voucher_id` (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='折旧记录表';

-- 资产盘点单表
CREATE TABLE IF NOT EXISTS `ast_stocktake` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `stocktake_no` VARCHAR(50) NOT NULL COMMENT '盘点单号',
  `stocktake_name` VARCHAR(200) NOT NULL COMMENT '盘点单名称',
  `stocktake_date` DATE NOT NULL COMMENT '盘点日期',
  `stocktake_person` VARCHAR(100) DEFAULT NULL COMMENT '盘点人员',
  `scope` VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT '盘点范围 ALL-全部 CATEGORY-按分类 SPECIFIC-指定资产',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '盘点状态 0-进行中 1-已完成 2-已作废',
  `total_count` INT NOT NULL DEFAULT 0 COMMENT '资产总数',
  `loss_count` INT NOT NULL DEFAULT 0 COMMENT '盘亏数量',
  `gain_count` INT NOT NULL DEFAULT 0 COMMENT '盘盈数量',
  `match_count` INT NOT NULL DEFAULT 0 COMMENT '一致数量',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_stocktake_no` (`stocktake_no`),
  KEY `idx_ast_stocktake_account_set` (`account_set_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产盘点单表';

-- 资产盘点明细表
CREATE TABLE IF NOT EXISTS `ast_stocktake_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stocktake_id` BIGINT NOT NULL COMMENT '盘点单ID',
  `asset_id` BIGINT NOT NULL COMMENT '固定资产ID',
  `asset_code` VARCHAR(50) NOT NULL COMMENT '资产编码',
  `asset_name` VARCHAR(200) NOT NULL COMMENT '资产名称',
  `book_quantity` DECIMAL(18,4) NOT NULL DEFAULT 0.0000 COMMENT '账面数量',
  `actual_quantity` DECIMAL(18,4) DEFAULT NULL COMMENT '实盘数量',
  `diff_quantity` DECIMAL(18,4) DEFAULT NULL COMMENT '差异数量（实盘-账面）',
  `book_value` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '账面原值',
  `actual_value` DECIMAL(18,2) DEFAULT NULL COMMENT '实盘原值',
  `diff_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '差异金额',
  `result` VARCHAR(20) DEFAULT NULL COMMENT '盘点结果 MATCH-一致 LOSS-盘亏 GAIN-盘盈',
  `handle_opinion` VARCHAR(500) DEFAULT NULL COMMENT '处理意见',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ast_stocktake_detail_stocktake` (`stocktake_id`),
  KEY `idx_ast_stocktake_detail_asset` (`asset_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产盘点明细表';

-- ============================================
-- 13. 客户管理模块
-- ============================================

-- 客户表
CREATE TABLE IF NOT EXISTS `cst_customer` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_customer_code` (`account_set_id`, `customer_code`),
  KEY `idx_customer_name` (`customer_name`),
  KEY `idx_customer_type` (`customer_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户表';

-- 服务合同表
CREATE TABLE IF NOT EXISTS `cst_service_contract` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_account_set_contract_no` (`account_set_id`, `contract_no`),
  KEY `idx_contract_customer_id` (`customer_id`),
  KEY `idx_contract_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='服务合同表';

-- 收款记录表
CREATE TABLE IF NOT EXISTS `cst_payment_record` (
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
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_contract_id` (`contract_id`),
  KEY `idx_payment_customer_id` (`customer_id`),
  KEY `idx_payment_date` (`payment_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='收款记录表';

-- 客户开票记录表（代账公司给客户开服务费发票）
CREATE TABLE IF NOT EXISTS `cst_billing_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `contract_id` BIGINT DEFAULT NULL COMMENT '合同ID',
  `billing_date` DATE NOT NULL COMMENT '开票日期',
  `invoice_no` VARCHAR(50) DEFAULT NULL COMMENT '发票号码',
  `invoice_type` TINYINT DEFAULT NULL COMMENT '发票类型 1-专票 2-普票 3-电子普票',
  `amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '开票金额（含税）',
  `tax_rate` DECIMAL(6,4) DEFAULT NULL COMMENT '税率',
  `tax_amount` DECIMAL(18,2) DEFAULT NULL COMMENT '税额',
  `amount_without_tax` DECIMAL(18,2) DEFAULT NULL COMMENT '不含税金额',
  `billing_content` VARCHAR(500) DEFAULT NULL COMMENT '开票内容/商品名称',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-已开票未收款 1-已收款 2-已作废',
  `payment_record_id` BIGINT DEFAULT NULL COMMENT '关联收款记录ID',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_billing_customer_id` (`customer_id`),
  KEY `idx_billing_contract_id` (`contract_id`),
  KEY `idx_billing_date` (`billing_date`),
  KEY `idx_billing_invoice_no` (`invoice_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户开票记录表';

-- ============================================
-- 13.5 工商服务模块
-- ============================================

-- 工商服务记录表
CREATE TABLE IF NOT EXISTS `ic_service` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `contract_id` BIGINT DEFAULT NULL COMMENT '合同ID',
  `service_type` TINYINT NOT NULL DEFAULT 1 COMMENT '服务类型 1-工商注册 2-工商变更 3-工商注销',
  `service_name` VARCHAR(200) DEFAULT NULL COMMENT '服务项目名称',
  `service_status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待派工 1-进行中 2-已完成 3-已取消',
  `assignee_id` BIGINT DEFAULT NULL COMMENT '经办人ID',
  `expected_complete_date` DATE DEFAULT NULL COMMENT '预计完成日期',
  `actual_complete_date` DATE DEFAULT NULL COMMENT '实际完成日期',
  `cost_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '成本金额',
  `service_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '服务金额',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ic_service_customer_id` (`customer_id`),
  KEY `idx_ic_service_status` (`service_status`),
  KEY `idx_ic_service_assignee` (`assignee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工商服务记录表';

-- 工商外勤任务表
CREATE TABLE IF NOT EXISTS `ic_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `service_id` BIGINT NOT NULL COMMENT '工商服务ID',
  `task_name` VARCHAR(200) NOT NULL COMMENT '任务名称',
  `task_status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待处理 1-进行中 2-已完成 3-已取消',
  `assignee_id` BIGINT DEFAULT NULL COMMENT '经办人ID',
  `field_date` DATE DEFAULT NULL COMMENT '外勤日期',
  `location` VARCHAR(200) DEFAULT NULL COMMENT '地点',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ic_task_service_id` (`service_id`),
  KEY `idx_ic_task_status` (`task_status`),
  KEY `idx_ic_task_assignee` (`assignee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='工商外勤任务表';

-- ============================================
-- 14. 期初余额模块
-- ============================================

-- 科目期初余额表
CREATE TABLE IF NOT EXISTS `acc_subject_balance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `subject_id` BIGINT NOT NULL COMMENT '科目ID',
  `subject_code` VARCHAR(50) NOT NULL COMMENT '科目编码',
  `subject_name` VARCHAR(200) NOT NULL COMMENT '科目名称',
  `year` INT NOT NULL COMMENT '年度',
  `period` INT NOT NULL DEFAULT 1 COMMENT '期次 1-期初',
  `begin_debit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '期初借方余额',
  `begin_credit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '期初贷方余额',
  `auxiliary_id` BIGINT DEFAULT NULL COMMENT '辅助核算ID',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁',
  `create_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` BIGINT DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_subject_balance` (`account_set_id`, `subject_id`, `year`, `period`),
  KEY `idx_sub_balance_year` (`account_set_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='科目期初余额表';

-- ============================================
-- 15. 凭证模板模块
-- ============================================

CREATE TABLE IF NOT EXISTS `acc_voucher_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID(0-全局)',
  `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `template_category` VARCHAR(50) DEFAULT NULL COMMENT '模板分类',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `voucher_word_id` BIGINT DEFAULT NULL COMMENT '凭证字ID',
  `attachment_count` INT DEFAULT 0 COMMENT '附件数',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `version` INT NOT NULL DEFAULT 0,
  `create_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` BIGINT DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_voucher_tpl_account_set` (`account_set_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='凭证模板表';

CREATE TABLE IF NOT EXISTS `acc_voucher_template_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_id` BIGINT NOT NULL COMMENT '模板ID',
  `line_no` INT NOT NULL COMMENT '行号',
  `summary` VARCHAR(500) DEFAULT NULL COMMENT '摘要',
  `subject_id` BIGINT DEFAULT NULL COMMENT '科目ID',
  `subject_code` VARCHAR(50) DEFAULT NULL COMMENT '科目编码',
  `subject_name` VARCHAR(200) DEFAULT NULL COMMENT '科目名称',
  `debit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '借方金额',
  `credit` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '贷方金额',
  `sort_order` INT NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_voucher_tpl_detail_id` (`template_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='凭证模板明细表';

-- ============================================
-- 16. 辅助核算模块
-- ============================================

CREATE TABLE IF NOT EXISTS `acc_auxiliary_category` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID(0-全局)',
  `category_code` VARCHAR(50) NOT NULL COMMENT '类别编码',
  `category_name` VARCHAR(100) NOT NULL COMMENT '类别名称',
  `category_type` VARCHAR(50) NOT NULL COMMENT '类型:客户/供应商/部门/员工/项目',
  `remark` VARCHAR(500) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `version` INT NOT NULL DEFAULT 0,
  `create_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` BIGINT DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_aux_category_code` (`account_set_id`, `category_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='辅助核算类别表';

CREATE TABLE IF NOT EXISTS `acc_auxiliary_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `category_id` BIGINT NOT NULL COMMENT '类别ID',
  `item_code` VARCHAR(50) NOT NULL COMMENT '项目编码',
  `item_name` VARCHAR(200) NOT NULL COMMENT '项目名称',
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父级ID',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
  `remark` VARCHAR(500) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `version` INT NOT NULL DEFAULT 0,
  `create_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` BIGINT DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_aux_item_code` (`account_set_id`, `category_id`, `item_code`),
  KEY `idx_aux_item_category` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='辅助核算项目表';

-- ============================================
-- 17. 凭证附件模块
-- ============================================

CREATE TABLE IF NOT EXISTS `acc_voucher_attachment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `voucher_id` BIGINT NOT NULL COMMENT '凭证ID',
  `file_name` VARCHAR(200) NOT NULL COMMENT '文件名',
  `file_path` VARCHAR(500) NOT NULL COMMENT '文件路径',
  `file_size` BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
  `file_type` VARCHAR(50) DEFAULT NULL COMMENT '文件类型',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_voucher_att_voucher_id` (`voucher_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='凭证附件表';

-- ============================================
-- 18. 发票管理模块
-- ============================================

CREATE TABLE IF NOT EXISTS `doc_input_invoice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `invoice_code` VARCHAR(50) DEFAULT NULL COMMENT '发票代码',
  `invoice_number` VARCHAR(50) NOT NULL COMMENT '发票号码',
  `invoice_date` DATE NOT NULL COMMENT '开票日期',
  `invoice_type` VARCHAR(50) NOT NULL COMMENT '发票类型:增值税专用发票/增值税普通发票/电子发票',
  `seller_name` VARCHAR(200) NOT NULL COMMENT '销方名称',
  `seller_tax_number` VARCHAR(50) DEFAULT NULL COMMENT '销方税号',
  `buyer_name` VARCHAR(200) DEFAULT NULL COMMENT '购方名称',
  `buyer_tax_number` VARCHAR(50) DEFAULT NULL COMMENT '购方税号',
  `amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '金额(不含税)',
  `tax_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '税额',
  `total_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '价税合计',
  `tax_rate` DECIMAL(10,4) DEFAULT NULL COMMENT '税率',
  `auth_status` TINYINT NOT NULL DEFAULT 0 COMMENT '认证状态 0-未认证 1-已认证 2-已作废',
  `auth_date` DATE DEFAULT NULL COMMENT '认证日期',
  `voucher_id` BIGINT DEFAULT NULL COMMENT '关联凭证ID',
  `remark` VARCHAR(500) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `version` INT NOT NULL DEFAULT 0,
  `create_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` BIGINT DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_input_invoice_no` (`account_set_id`, `invoice_number`),
  KEY `idx_input_invoice_date` (`invoice_date`),
  KEY `idx_input_invoice_auth` (`auth_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='进项发票表';

CREATE TABLE IF NOT EXISTS `doc_output_invoice` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `invoice_code` VARCHAR(50) DEFAULT NULL COMMENT '发票代码',
  `invoice_number` VARCHAR(50) NOT NULL COMMENT '发票号码',
  `invoice_date` DATE NOT NULL COMMENT '开票日期',
  `invoice_type` VARCHAR(50) NOT NULL COMMENT '发票类型',
  `buyer_name` VARCHAR(200) NOT NULL COMMENT '购方名称',
  `buyer_tax_number` VARCHAR(50) DEFAULT NULL COMMENT '购方税号',
  `seller_name` VARCHAR(200) DEFAULT NULL COMMENT '销方名称',
  `seller_tax_number` VARCHAR(50) DEFAULT NULL COMMENT '销方税号',
  `amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '金额(不含税)',
  `tax_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '税额',
  `total_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '价税合计',
  `tax_rate` DECIMAL(10,4) DEFAULT NULL COMMENT '税率',
  `invoice_status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-正常 1-已作废 2-已红冲',
  `voucher_id` BIGINT DEFAULT NULL COMMENT '关联凭证ID',
  `remark` VARCHAR(500) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `version` INT NOT NULL DEFAULT 0,
  `create_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` BIGINT DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_output_invoice_no` (`account_set_id`, `invoice_number`),
  KEY `idx_output_invoice_date` (`invoice_date`),
  KEY `idx_output_invoice_status` (`invoice_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='销项发票表';

-- ============================================
-- 19. 代账服务流程模块
-- ============================================

CREATE TABLE IF NOT EXISTS `biz_service_flow_node` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `node_code` VARCHAR(50) NOT NULL COMMENT '节点编码',
  `node_name` VARCHAR(100) NOT NULL COMMENT '节点名称',
  `sort_order` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `description` VARCHAR(500) DEFAULT NULL COMMENT '描述',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_service_flow_node_code` (`node_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代账服务流程节点表';

CREATE TABLE IF NOT EXISTS `biz_service_task` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `customer_id` BIGINT DEFAULT NULL COMMENT '客户ID',
  `year` INT NOT NULL COMMENT '年度',
  `month` INT NOT NULL COMMENT '月份',
  `node_id` BIGINT NOT NULL COMMENT '流程节点ID',
  `node_name` VARCHAR(100) DEFAULT NULL COMMENT '节点名称',
  `assignee_id` BIGINT DEFAULT NULL COMMENT '指派人ID',
  `assignee_name` VARCHAR(50) DEFAULT NULL COMMENT '指派人姓名',
  `task_status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-待处理 1-进行中 2-已完成',
  `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `remark` VARCHAR(500) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` BIGINT DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_service_task_period` (`account_set_id`, `year`, `month`),
  KEY `idx_service_task_assignee` (`assignee_id`),
  KEY `idx_service_task_status` (`task_status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='代账服务任务表';

-- ============================================
-- 20. 部门与岗位模块
-- ============================================

CREATE TABLE IF NOT EXISTS `sys_department` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `parent_id` BIGINT NOT NULL DEFAULT 0 COMMENT '父级ID',
  `dept_code` VARCHAR(50) NOT NULL COMMENT '部门编码',
  `dept_name` VARCHAR(100) NOT NULL COMMENT '部门名称',
  `manager_id` BIGINT DEFAULT NULL COMMENT '部门负责人ID',
  `sort_order` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_dept_code` (`dept_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='部门表';

CREATE TABLE IF NOT EXISTS `sys_position` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `position_code` VARCHAR(50) NOT NULL COMMENT '岗位编码',
  `position_name` VARCHAR(100) NOT NULL COMMENT '岗位名称',
  `department_id` BIGINT DEFAULT NULL COMMENT '所属部门ID',
  `description` VARCHAR(500) DEFAULT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_position_code` (`position_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='岗位表';

-- ============================================
-- 21. 税务风险预警模块
-- ============================================

CREATE TABLE IF NOT EXISTS `tax_risk_warning` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `year` INT NOT NULL COMMENT '年度',
  `month` INT NOT NULL COMMENT '月份',
  `risk_type` VARCHAR(50) NOT NULL COMMENT '风险类型:税负率/发票/申报',
  `risk_level` TINYINT NOT NULL DEFAULT 1 COMMENT '风险等级 1-低 2-中 3-高',
  `risk_description` VARCHAR(500) NOT NULL COMMENT '风险描述',
  `risk_value` VARCHAR(200) DEFAULT NULL COMMENT '风险值',
  `suggestion` VARCHAR(500) DEFAULT NULL COMMENT '处理建议',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-未处理 1-已处理 2-已忽略',
  `handle_remark` VARCHAR(500) DEFAULT NULL COMMENT '处理备注',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_tax_risk_period` (`account_set_id`, `year`, `month`),
  KEY `idx_tax_risk_level` (`risk_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='税务风险预警表';

-- ============================================
-- 22. 登录日志模块
-- ============================================

CREATE TABLE IF NOT EXISTS `sys_login_log` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL COMMENT '用户名',
  `user_id` BIGINT DEFAULT NULL COMMENT '用户ID',
  `login_type` TINYINT NOT NULL DEFAULT 1 COMMENT '登录类型 1-登录 2-登出',
  `login_status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-失败 1-成功',
  `login_ip` VARCHAR(50) DEFAULT NULL COMMENT '登录IP',
  `login_location` VARCHAR(200) DEFAULT NULL COMMENT '登录地点',
  `browser` VARCHAR(100) DEFAULT NULL COMMENT '浏览器',
  `os` VARCHAR(100) DEFAULT NULL COMMENT '操作系统',
  `message` VARCHAR(500) DEFAULT NULL COMMENT '提示消息',
  `login_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '登录时间',
  PRIMARY KEY (`id`),
  KEY `idx_login_log_user` (`username`),
  KEY `idx_login_log_time` (`login_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='登录日志表';

-- ============================================
-- 23. 系统设置模块
-- ============================================

CREATE TABLE IF NOT EXISTS `sys_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `config_key` VARCHAR(100) NOT NULL COMMENT '参数键',
  `config_value` VARCHAR(2000) DEFAULT NULL COMMENT '参数值',
  `config_name` VARCHAR(100) DEFAULT NULL COMMENT '参数名称',
  `config_type` VARCHAR(50) DEFAULT NULL COMMENT '参数类型',
  `remark` VARCHAR(500) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` BIGINT DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='系统设置表';

-- 兼容已存在的sys_config表（若缺少version列则补充）
ALTER TABLE `sys_config` ADD COLUMN IF NOT EXISTS `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

-- ============================================
-- 24. CRM商机管理模块
-- ============================================

CREATE TABLE IF NOT EXISTS `crm_opportunity` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `opportunity_name` VARCHAR(200) NOT NULL COMMENT '商机名称',
  `customer_name` VARCHAR(200) DEFAULT NULL COMMENT '客户名称',
  `contact_person` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` VARCHAR(20) DEFAULT NULL COMMENT '联系电话',
  `source` VARCHAR(50) DEFAULT NULL COMMENT '商机来源',
  `stage` VARCHAR(50) NOT NULL DEFAULT '线索' COMMENT '阶段:线索/跟进/报价/谈判/成交/流失',
  `expected_amount` DECIMAL(18,2) DEFAULT 0.00 COMMENT '预计金额',
  `expected_close_date` DATE DEFAULT NULL COMMENT '预计成交日期',
  `assignee_id` BIGINT DEFAULT NULL COMMENT '负责人ID',
  `assignee_name` VARCHAR(50) DEFAULT NULL COMMENT '负责人姓名',
  `remark` VARCHAR(500) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` BIGINT DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_crm_opp_stage` (`stage`),
  KEY `idx_crm_opp_assignee` (`assignee_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商机管理表';

-- ============================================
-- 25. 客户服务报告模块
-- ============================================

CREATE TABLE IF NOT EXISTS `cst_service_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `customer_id` BIGINT DEFAULT NULL COMMENT '客户ID',
  `report_year` INT NOT NULL COMMENT '报告年度',
  `report_month` INT DEFAULT NULL COMMENT '报告月份(空=年度报告)',
  `report_type` VARCHAR(50) NOT NULL COMMENT '报告类型:月度/季度/年度',
  `total_revenue` DECIMAL(18,2) DEFAULT 0.00 COMMENT '总收入',
  `total_expense` DECIMAL(18,2) DEFAULT 0.00 COMMENT '总支出',
  `net_profit` DECIMAL(18,2) DEFAULT 0.00 COMMENT '净利润',
  `tax_amount` DECIMAL(18,2) DEFAULT 0.00 COMMENT '纳税总额',
  `financial_summary` TEXT DEFAULT NULL COMMENT '财务摘要',
  `risk_warning` TEXT DEFAULT NULL COMMENT '风险提示',
  `suggestion` TEXT DEFAULT NULL COMMENT '经营建议',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-草稿 1-已发布',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` BIGINT DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_service_report_customer` (`customer_id`, `report_year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户服务报告表';

-- ============================================
-- 26. 自定义报表模块
-- ============================================

CREATE TABLE IF NOT EXISTS `rpt_custom_report` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `report_name` VARCHAR(200) NOT NULL COMMENT '报表名称',
  `report_code` VARCHAR(50) NOT NULL COMMENT '报表编码',
  `report_type` VARCHAR(50) DEFAULT NULL COMMENT '报表类型',
  `description` VARCHAR(500) DEFAULT NULL,
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` BIGINT DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_custom_report_code` (`report_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自定义报表表';

CREATE TABLE IF NOT EXISTS `rpt_custom_report_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `report_id` BIGINT NOT NULL COMMENT '报表ID',
  `row_no` INT NOT NULL COMMENT '行号',
  `item_name` VARCHAR(200) NOT NULL COMMENT '项目名称',
  `formula` VARCHAR(1000) DEFAULT NULL COMMENT '取数公式(科目编码组合)',
  `display_direction` TINYINT DEFAULT 0 COMMENT '显示方向 0-借 1-贷',
  `is_total` TINYINT NOT NULL DEFAULT 0 COMMENT '是否合计行',
  `parent_row_no` INT DEFAULT NULL COMMENT '父行号',
  PRIMARY KEY (`id`),
  KEY `idx_custom_report_item_id` (`report_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='自定义报表项目表';

-- ============================================
-- 27. 资产变动记录模块
-- ============================================

CREATE TABLE IF NOT EXISTS `asset_change_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `asset_id` BIGINT NOT NULL COMMENT '资产ID',
  `change_type` VARCHAR(50) NOT NULL COMMENT '变动类型:购入/出售/报废/调拨/盘亏',
  `change_date` DATE NOT NULL COMMENT '变动日期',
  `change_amount` DECIMAL(18,2) DEFAULT 0.00 COMMENT '变动金额',
  `from_department` VARCHAR(100) DEFAULT NULL COMMENT '原部门',
  `to_department` VARCHAR(100) DEFAULT NULL COMMENT '新部门',
  `voucher_id` BIGINT DEFAULT NULL COMMENT '关联凭证ID',
  `remark` VARCHAR(500) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_asset_change_asset_id` (`asset_id`),
  KEY `idx_asset_change_date` (`change_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='资产变动记录表';

-- ============================================
-- 28. 长期待摊费用模块
-- ============================================

CREATE TABLE IF NOT EXISTS `acc_amortization` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `amortization_name` VARCHAR(200) NOT NULL COMMENT '费用名称',
  `subject_id` BIGINT DEFAULT NULL COMMENT '科目ID',
  `total_amount` DECIMAL(18,2) NOT NULL COMMENT '待摊总额',
  `amortized_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '已摊销额',
  `remaining_amount` DECIMAL(18,2) NOT NULL DEFAULT 0.00 COMMENT '剩余待摊',
  `start_date` DATE NOT NULL COMMENT '开始日期',
  `end_date` DATE NOT NULL COMMENT '结束日期',
  `total_months` INT NOT NULL COMMENT '总月数',
  `monthly_amount` DECIMAL(18,2) NOT NULL COMMENT '月摊销额',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态 0-摊销中 1-已摊完',
  `remark` VARCHAR(500) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `version` INT NOT NULL DEFAULT 0,
  `create_by` BIGINT DEFAULT NULL,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_by` BIGINT DEFAULT NULL,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_amortization_account_set` (`account_set_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='长期待摊费用表';

-- 兼容已有库:为长期待摊费用表补充"最近摊销期间"列,防止同一期间重复摊销
ALTER TABLE `acc_amortization` ADD COLUMN IF NOT EXISTS `last_amortized_period` VARCHAR(7) DEFAULT NULL COMMENT '最近摊销期间(yyyy-MM)';

-- ============================================
-- 29. 薪资公式与社保配置模块
-- ============================================

CREATE TABLE IF NOT EXISTS `sal_salary_formula` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `formula_name` VARCHAR(100) NOT NULL COMMENT '公式名称',
  `target_item` VARCHAR(50) NOT NULL COMMENT '目标薪资项',
  `formula_expression` VARCHAR(1000) NOT NULL COMMENT '公式表达式',
  `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级',
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_salary_formula_account_set` (`account_set_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='薪资公式表';

CREATE TABLE IF NOT EXISTS `sal_social_security_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `year` INT NOT NULL COMMENT '年度',
  `city` VARCHAR(50) DEFAULT NULL COMMENT '城市',
  `pension_employer` DECIMAL(10,4) NOT NULL DEFAULT 0.16 COMMENT '养老单位比例',
  `pension_employee` DECIMAL(10,4) NOT NULL DEFAULT 0.08 COMMENT '养老个人比例',
  `medical_employer` DECIMAL(10,4) NOT NULL DEFAULT 0.08 COMMENT '医疗单位比例',
  `medical_employee` DECIMAL(10,4) NOT NULL DEFAULT 0.02 COMMENT '医疗个人比例',
  `unemployment_employer` DECIMAL(10,4) NOT NULL DEFAULT 0.005 COMMENT '失业单位比例',
  `unemployment_employee` DECIMAL(10,4) NOT NULL DEFAULT 0.005 COMMENT '失业个人比例',
  `injury_employer` DECIMAL(10,4) NOT NULL DEFAULT 0.002 COMMENT '工伤单位比例',
  `maternity_employer` DECIMAL(10,4) NOT NULL DEFAULT 0.008 COMMENT '生育单位比例',
  `housing_fund_employer` DECIMAL(10,4) NOT NULL DEFAULT 0.07 COMMENT '公积金单位比例',
  `housing_fund_employee` DECIMAL(10,4) NOT NULL DEFAULT 0.07 COMMENT '公积金个人比例',
  `base_lower` DECIMAL(18,2) DEFAULT NULL COMMENT '缴费基数下限',
  `base_upper` DECIMAL(18,2) DEFAULT NULL COMMENT '缴费基数上限',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_social_security` (`account_set_id`, `year`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='社保公积金配置表';

-- 个税专项附加扣除表
CREATE TABLE IF NOT EXISTS `sal_special_deduction` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `employee_id` BIGINT NOT NULL COMMENT '员工ID',
  `employee_name` VARCHAR(50) DEFAULT NULL COMMENT '员工姓名',
  `deduction_type` VARCHAR(30) NOT NULL COMMENT '扣除项目类型',
  `deduction_name` VARCHAR(100) DEFAULT NULL COMMENT '扣除项目名称',
  `monthly_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '月度扣除标准金额',
  `annual_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '年度扣除金额(大病医疗用)',
  `effective_from` DATE DEFAULT NULL COMMENT '有效起始月份',
  `effective_to` DATE DEFAULT NULL COMMENT '有效截止月份',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-停用 1-生效中',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_sal_special_deduction_account_set_id` (`account_set_id`),
  KEY `idx_sal_special_deduction_employee_id` (`employee_id`),
  KEY `idx_deduction_type` (`deduction_type`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='个税专项附加扣除表';

-- ============================================
-- 30. 多栏账配置模块
-- ============================================

CREATE TABLE IF NOT EXISTS `acc_multi_column_config` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `subject_id` BIGINT NOT NULL COMMENT '科目ID',
  `subject_code` VARCHAR(50) NOT NULL COMMENT '科目编码',
  `config_name` VARCHAR(100) NOT NULL COMMENT '配置名称',
  `column_items` VARCHAR(1000) NOT NULL COMMENT '栏目项(逗号分隔)',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_multi_col_subject` (`account_set_id`, `subject_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='多栏账配置表';

-- ============================================
-- 31. 行业模板模块
-- ============================================

CREATE TABLE IF NOT EXISTS `sys_industry_template` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `template_code` VARCHAR(50) NOT NULL COMMENT '模板编码',
  `template_name` VARCHAR(100) NOT NULL COMMENT '模板名称',
  `industry_type` VARCHAR(50) NOT NULL COMMENT '行业类型',
  `accounting_standard` VARCHAR(50) NOT NULL COMMENT '会计准则',
  `description` VARCHAR(500) DEFAULT NULL,
  `subject_config` TEXT DEFAULT NULL COMMENT '科目配置(JSON)',
  `status` TINYINT NOT NULL DEFAULT 1,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_industry_template_code` (`template_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='行业模板表';

-- ============================================
-- 32. 客户看账门户模块
-- ============================================

CREATE TABLE IF NOT EXISTS `cst_portal_account` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `portal_username` VARCHAR(50) NOT NULL COMMENT '门户用户名',
  `portal_password` VARCHAR(100) NOT NULL COMMENT '门户密码(加密)',
  `expire_date` DATE DEFAULT NULL COMMENT '到期日期',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态 0-禁用 1-正常',
  `last_login_time` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_portal_username` (`portal_username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='客户看账门户表';

-- ============================================
-- 33. 员工绩效模块
-- ============================================

CREATE TABLE IF NOT EXISTS `sys_employee_performance` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL COMMENT '员工ID',
  `user_name` VARCHAR(50) NOT NULL COMMENT '员工姓名',
  `year` INT NOT NULL COMMENT '年度',
  `month` INT NOT NULL COMMENT '月份',
  `voucher_count` INT NOT NULL DEFAULT 0 COMMENT '凭证录入数',
  `audit_count` INT NOT NULL DEFAULT 0 COMMENT '审核数',
  `task_complete_count` INT NOT NULL DEFAULT 0 COMMENT '任务完成数',
  `customer_count` INT NOT NULL DEFAULT 0 COMMENT '服务客户数',
  `performance_score` DECIMAL(5,2) DEFAULT 0.00 COMMENT '绩效分数',
  `remark` VARCHAR(500) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_performance_user_period` (`user_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='员工绩效表';

-- ============================================
-- 客户分类与状态管理字段扩展
-- ============================================
ALTER TABLE `cst_customer` ADD COLUMN IF NOT EXISTS `customer_level` VARCHAR(50) DEFAULT NULL COMMENT '客户等级';
ALTER TABLE `cst_customer` ADD COLUMN IF NOT EXISTS `industry_type` VARCHAR(50) DEFAULT NULL COMMENT '行业类型';
ALTER TABLE `cst_customer` ADD COLUMN IF NOT EXISTS `company_size` VARCHAR(50) DEFAULT NULL COMMENT '企业规模';
ALTER TABLE `cst_customer` ADD COLUMN IF NOT EXISTS `taxpayer_type` VARCHAR(50) DEFAULT NULL COMMENT '纳税人类型';
ALTER TABLE `cst_customer` ADD COLUMN IF NOT EXISTS `customer_status` TINYINT DEFAULT 1 COMMENT '客户状态 0-潜在 1-在服 2-流失';
ALTER TABLE `cst_customer` ADD COLUMN IF NOT EXISTS `service_start_date` DATE DEFAULT NULL COMMENT '服务开始日期';
ALTER TABLE `cst_customer` ADD COLUMN IF NOT EXISTS `service_end_date` DATE DEFAULT NULL COMMENT '服务结束日期';
ALTER TABLE `cst_customer` ADD COLUMN IF NOT EXISTS `credit_limit` DECIMAL(18,2) DEFAULT 0 COMMENT '信用额度';
ALTER TABLE `cst_customer` ADD COLUMN IF NOT EXISTS `contact_count` INT DEFAULT 0 COMMENT '联系人数量';

-- ============================================
-- 凭证草稿状态字段扩展
-- ============================================
ALTER TABLE `acc_voucher` ADD COLUMN IF NOT EXISTS `draft_status` TINYINT DEFAULT 0 COMMENT '草稿状态 0-正常 1-草稿';

-- ============================================
-- 现金流量表调整表
-- ============================================
CREATE TABLE IF NOT EXISTS `rpt_cash_flow_adjustment` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `account_set_id` BIGINT NOT NULL,
  `year` INT NOT NULL,
  `month` INT NOT NULL,
  `item_name` VARCHAR(200) NOT NULL,
  `category` VARCHAR(50) NOT NULL,
  `original_amount` DECIMAL(18,2) DEFAULT 0,
  `adjusted_amount` DECIMAL(18,2) DEFAULT 0,
  `adjustment_reason` VARCHAR(500) DEFAULT NULL,
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cash_flow_adj_period` (`account_set_id`, `year`, `month`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='现金流量表调整表';

-- ============================================
-- 34. 汇率管理模块
-- ============================================

CREATE TABLE IF NOT EXISTS `sys_exchange_rate` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `currency_code` VARCHAR(10) NOT NULL COMMENT '币种代码',
  `currency_name` VARCHAR(50) NOT NULL COMMENT '币种名称',
  `rate` DECIMAL(18,6) NOT NULL COMMENT '汇率',
  `rate_date` DATE NOT NULL COMMENT '汇率日期',
  `rate_type` VARCHAR(20) DEFAULT '中间价' COMMENT '汇率类型',
  `deleted` TINYINT NOT NULL DEFAULT 0,
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_exchange_rate_date` (`rate_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='汇率表';

-- ============================================
-- 18. 库存管理模块
-- ============================================

-- 商品/物料表
CREATE TABLE IF NOT EXISTS `inv_item` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `item_code` VARCHAR(50) NOT NULL COMMENT '商品编码',
  `item_name` VARCHAR(200) NOT NULL COMMENT '商品名称',
  `specification` VARCHAR(200) COMMENT '规格型号',
  `unit` VARCHAR(20) COMMENT '计量单位',
  `unit_price` DECIMAL(18,4) COMMENT '参考单价',
  `category` VARCHAR(50) COMMENT '商品分类',
  `status` TINYINT DEFAULT 1 COMMENT '状态:0禁用1启用',
  `remark` VARCHAR(500) COMMENT '备注',
  `create_by` BIGINT COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_inv_item_account_set` (`account_set_id`),
  KEY `idx_inv_item_code` (`item_code`),
  KEY `idx_inv_item_name` (`item_name`),
  KEY `idx_inv_item_category` (`category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='商品物料表';

-- 库存余额表（按月）
CREATE TABLE IF NOT EXISTS `inv_stock` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `item_id` BIGINT NOT NULL COMMENT '商品ID',
  `year` INT NOT NULL COMMENT '年度',
  `month` INT NOT NULL COMMENT '月份',
  `begin_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '期初数量',
  `begin_amount` DECIMAL(18,2) DEFAULT 0 COMMENT '期初金额',
  `in_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '本期入库数量',
  `in_amount` DECIMAL(18,2) DEFAULT 0 COMMENT '本期入库金额',
  `out_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '本期出库数量',
  `out_amount` DECIMAL(18,2) DEFAULT 0 COMMENT '本期出库金额',
  `end_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '期末数量',
  `end_amount` DECIMAL(18,2) DEFAULT 0 COMMENT '期末金额',
  `unit_cost` DECIMAL(18,6) DEFAULT 0 COMMENT '单位成本',
  `version` INT DEFAULT 0 COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inv_stock_item_period` (`account_set_id`, `item_id`, `year`, `month`),
  KEY `idx_inv_stock_item` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='库存余额表';

-- 入库单表
CREATE TABLE IF NOT EXISTS `inv_in` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `in_no` VARCHAR(30) NOT NULL COMMENT '入库单号',
  `in_type` TINYINT DEFAULT 1 COMMENT '入库类型:1采购入库2盘盈3其他入库',
  `in_date` DATE NOT NULL COMMENT '入库日期',
  `supplier` VARCHAR(200) COMMENT '供应商',
  `total_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '总数量',
  `total_amount` DECIMAL(18,2) DEFAULT 0 COMMENT '总金额',
  `status` TINYINT DEFAULT 0 COMMENT '状态:0待审核1已审核',
  `remark` VARCHAR(500) COMMENT '备注',
  `create_by` BIGINT COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `voucher_id` BIGINT DEFAULT NULL COMMENT '关联凭证ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inv_in_no` (`in_no`),
  KEY `idx_inv_in_account_set` (`account_set_id`),
  KEY `idx_inv_in_date` (`in_date`),
  KEY `idx_inv_in_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='入库单表';

-- 入库单明细表
CREATE TABLE IF NOT EXISTS `inv_in_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `in_id` BIGINT NOT NULL COMMENT '入库单ID',
  `item_id` BIGINT NOT NULL COMMENT '商品ID',
  `item_code` VARCHAR(50) COMMENT '商品编码',
  `item_name` VARCHAR(200) COMMENT '商品名称',
  `specification` VARCHAR(200) COMMENT '规格型号',
  `unit` VARCHAR(20) COMMENT '单位',
  `quantity` DECIMAL(18,4) NOT NULL COMMENT '数量',
  `unit_price` DECIMAL(18,4) NOT NULL COMMENT '单价',
  `amount` DECIMAL(18,2) NOT NULL COMMENT '金额',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_inv_in_detail_in_id` (`in_id`),
  KEY `idx_inv_in_detail_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='入库单明细表';

-- 出库单表
CREATE TABLE IF NOT EXISTS `inv_out` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `account_set_id` BIGINT NOT NULL COMMENT '账套ID',
  `out_no` VARCHAR(30) NOT NULL COMMENT '出库单号',
  `out_type` TINYINT DEFAULT 1 COMMENT '出库类型:1销售出库2盘亏3其他出库',
  `out_date` DATE NOT NULL COMMENT '出库日期',
  `customer` VARCHAR(200) COMMENT '客户',
  `total_quantity` DECIMAL(18,4) DEFAULT 0 COMMENT '总数量',
  `total_amount` DECIMAL(18,2) DEFAULT 0 COMMENT '总金额',
  `cost_amount` DECIMAL(18,2) DEFAULT 0 COMMENT '成本金额',
  `status` TINYINT DEFAULT 0 COMMENT '状态:0待审核1已审核',
  `remark` VARCHAR(500) COMMENT '备注',
  `create_by` BIGINT COMMENT '创建人ID',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  `voucher_id` BIGINT DEFAULT NULL COMMENT '关联凭证ID',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inv_out_no` (`out_no`),
  KEY `idx_inv_out_account_set` (`account_set_id`),
  KEY `idx_inv_out_date` (`out_date`),
  KEY `idx_inv_out_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='出库单表';

-- 出库单明细表
CREATE TABLE IF NOT EXISTS `inv_out_detail` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `out_id` BIGINT NOT NULL COMMENT '出库单ID',
  `item_id` BIGINT NOT NULL COMMENT '商品ID',
  `item_code` VARCHAR(50) COMMENT '商品编码',
  `item_name` VARCHAR(200) COMMENT '商品名称',
  `specification` VARCHAR(200) COMMENT '规格型号',
  `unit` VARCHAR(20) COMMENT '单位',
  `quantity` DECIMAL(18,4) NOT NULL COMMENT '数量',
  `unit_price` DECIMAL(18,4) NOT NULL COMMENT '售价',
  `amount` DECIMAL(18,2) NOT NULL COMMENT '售价金额',
  `unit_cost` DECIMAL(18,4) DEFAULT 0 COMMENT '单位成本',
  `cost_amount` DECIMAL(18,2) DEFAULT 0 COMMENT '成本金额',
  `remark` VARCHAR(500) COMMENT '备注',
  PRIMARY KEY (`id`),
  KEY `idx_inv_out_detail_out_id` (`out_id`),
  KEY `idx_inv_out_detail_item_id` (`item_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='出库单明细表';

-- ============================================
-- 18. 凭证模板 + 常用摘要库模块
-- ============================================

-- 凭证模板表
CREATE TABLE IF NOT EXISTS `voucher_template` (
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

-- 常用摘要库
CREATE TABLE IF NOT EXISTS `abstract_library` (
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

-- ============================================
-- AI 智能凭证识别相关表
-- ============================================

-- AI记账规则库(关键词->借贷科目映射,优先于AI调用,命中直接返回)
CREATE TABLE IF NOT EXISTS `ai_accounting_rule` (
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

-- AI识别反馈记录(用户实际选择 vs AI建议,用于闭环学习)
CREATE TABLE IF NOT EXISTS `ai_recognition_feedback` (
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

-- 用户账套偏好(收藏/最近访问/排序,用于顶部账套切换器记忆最近访问+收藏置顶)
CREATE TABLE IF NOT EXISTS `user_account_set_preference` (
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

-- ============================================
-- 36. 税负预警基准模块
-- ============================================

-- 行业税负率基准表
CREATE TABLE IF NOT EXISTS `tax_benchmark` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `industry_code` VARCHAR(20) NOT NULL COMMENT '行业代码',
  `industry_name` VARCHAR(100) NOT NULL COMMENT '行业名称',
  `vat_benchmark_rate` DECIMAL(5,4) NOT NULL DEFAULT 0.0300 COMMENT '增值税税负率基准(0.0300 = 3.00%)',
  `vat_warning_low` DECIMAL(5,4) NOT NULL DEFAULT 0.0200 COMMENT '增值税税负率下限预警',
  `vat_warning_high` DECIMAL(5,4) NOT NULL DEFAULT 0.0450 COMMENT '增值税税负率上限预警',
  `eit_benchmark_rate` DECIMAL(5,4) NOT NULL DEFAULT 0.0200 COMMENT '企业所得税税负率基准',
  `eit_warning_low` DECIMAL(5,4) NOT NULL DEFAULT 0.0120 COMMENT '企业所得税税负率下限预警',
  `eit_warning_high` DECIMAL(5,4) NOT NULL DEFAULT 0.0350 COMMENT '企业所得税税负率上限预警',
  `deleted` TINYINT NOT NULL DEFAULT 0 COMMENT '删除标志 0-未删除 1-已删除',
  `version` INT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号',
  `create_by` BIGINT DEFAULT NULL COMMENT '创建人',
  `create_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT DEFAULT NULL COMMENT '更新人',
  `update_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_industry_code` (`industry_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='行业税负率基准表';
