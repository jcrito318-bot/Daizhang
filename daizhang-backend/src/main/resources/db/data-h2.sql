-- ============================================
-- 代账系统初始化数据脚本 (H2 兼容版, MODE=MySQL)
-- 基于 init.sql 转换
-- 使用 MERGE INTO 语法：存在则更新，不存在则插入
-- ============================================

-- 默认管理员用户：仅在首次初始化(表中无 id=1 记录)时插入，
-- 不再使用 MERGE INTO 每次启动回写，避免覆盖运维已修改的密码。
-- 安全提示：默认口令为弱口令，部署后请立即登录并修改密码。
INSERT INTO `sys_user` (`id`, `username`, `password`, `real_name`, `phone`, `email`, `status`, `deleted`, `version`, `create_by`, `create_time`, `update_by`, `update_time`)
SELECT 1, 'admin', '$2a$10$4zBBUTSD7XrO8SgGwKj56ecSYyfGLEYjW067Ft9/SQM.Auu3ctGDa', '系统管理员', '13800138000', 'admin@daizhang.com', 1, 0, 0, 1, NOW(), 1, NOW()
WHERE NOT EXISTS (SELECT 1 FROM `sys_user` WHERE `id` = 1);

-- 默认角色
MERGE INTO `sys_role` (`id`, `role_name`, `role_code`, `description`, `status`, `deleted`, `version`, `create_by`, `create_time`, `update_by`, `update_time`) KEY (`id`) VALUES
(1, '管理员', 'ADMIN', '系统管理员，拥有所有权限', 1, 0, 0, 1, NOW(), 1, NOW()),
(2, '代账主管', 'MANAGER', '代账主管，管理代账业务', 1, 0, 0, 1, NOW(), 1, NOW()),
(3, '会计', 'ACCOUNTANT', '会计人员，负责账务处理', 1, 0, 0, 1, NOW(), 1, NOW()),
(4, '出纳', 'CASHIER', '出纳人员，负责资金管理', 1, 0, 0, 1, NOW(), 1, NOW());

-- 默认菜单
MERGE INTO `sys_menu` (`id`, `parent_id`, `name`, `path`, `component`, `icon`, `sort_order`, `menu_type`, `permission`, `visible`, `status`, `deleted`, `version`, `create_by`, `create_time`, `update_by`, `update_time`) KEY (`id`) VALUES
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
MERGE INTO `sys_user_role` (`id`, `user_id`, `role_id`) KEY (`id`) VALUES
(1, 1, 1);

-- 管理员角色菜单关联 (所有菜单)
MERGE INTO `sys_role_menu` (`id`, `role_id`, `menu_id`) KEY (`id`) VALUES
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
MERGE INTO `acc_voucher_word` (`id`, `account_set_id`, `name`, `code`, `sort_order`, `status`, `deleted`, `version`, `create_by`, `create_time`, `update_by`, `update_time`) KEY (`id`) VALUES
(1, 0, '收', '收', 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(2, 0, '付', '付', 2, 1, 0, 0, 1, NOW(), 1, NOW()),
(3, 0, '转', '转', 3, 1, 0, 0, 1, NOW(), 1, NOW()),
(4, 0, '记', '记', 4, 1, 0, 0, 1, NOW(), 1, NOW());

-- ============================================
-- 标准科目模板 (account_set_id=0 表示系统默认模板)
-- ============================================

-- 资产类科目
MERGE INTO `acc_subject` (`id`, `account_set_id`, `code`, `name`, `category`, `parent_id`, `level`, `balance_direction`, `is_auxiliary`, `is_cash`, `is_bank`, `is_current`, `status`, `deleted`, `version`, `create_by`, `create_time`, `update_by`, `update_time`) KEY (`id`) VALUES
-- 一级科目
(1, 0, '1001', '库存现金', '资产', 0, 1, 1, 0, 1, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(2, 0, '1002', '银行存款', '资产', 0, 1, 1, 0, 0, 1, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(3, 0, '1012', '其他货币资金', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(4, 0, '1101', '短期投资', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(5, 0, '1121', '应收票据', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(6, 0, '1122', '应收账款', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(7, 0, '1123', '预付账款', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(8, 0, '1131', '应收股利', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(9, 0, '1132', '应收利息', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(10, 0, '1221', '其他应收款', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(11, 0, '1401', '材料采购', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(12, 0, '1403', '原材料', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(13, 0, '1405', '库存商品', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(14, 0, '1501', '持有至到期投资', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(15, 0, '1601', '长期股权投资', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(16, 0, '1604', '投资性房地产', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(17, 0, '1701', '固定资产', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(18, 0, '1702', '累计折旧', '资产', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(19, 0, '1703', '在建工程', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(20, 0, '1704', '工程物资', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(21, 0, '1801', '无形资产', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(22, 0, '1802', '累计摊销', '资产', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(23, 0, '1901', '长期待摊费用', '资产', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 负债类科目
(24, 0, '2001', '短期借款', '负债', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(25, 0, '2201', '应付票据', '负债', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(26, 0, '2202', '应付账款', '负债', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(27, 0, '2203', '预收账款', '负债', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(28, 0, '2211', '应付职工薪酬', '负债', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(29, 0, '2221', '应交税费', '负债', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(30, 0, '2231', '应付利息', '负债', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(31, 0, '2232', '应付利润', '负债', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(32, 0, '2241', '其他应付款', '负债', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(33, 0, '2501', '长期借款', '负债', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(34, 0, '2502', '长期应付款', '负债', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 所有者权益类科目
(35, 0, '3001', '实收资本', '所有者权益', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(36, 0, '3002', '资本公积', '所有者权益', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(37, 0, '3101', '盈余公积', '所有者权益', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(38, 0, '3103', '本年利润', '所有者权益', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(39, 0, '3104', '利润分配', '所有者权益', 0, 1, 2, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 成本类科目
(40, 0, '4001', '生产成本', '成本', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),
(41, 0, '4101', '制造费用', '成本', 0, 1, 1, 0, 0, 0, 0, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 损益类科目 - 收入
(42, 0, '5001', '主营业务收入', '损益', 0, 1, 2, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(43, 0, '5051', '其他业务收入', '损益', 0, 1, 2, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(44, 0, '5301', '营业外收入', '损益', 0, 1, 2, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(45, 0, '5111', '投资收益', '损益', 0, 1, 2, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),

-- 损益类科目 - 费用
(46, 0, '5401', '主营业务成本', '损益', 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(47, 0, '5402', '其他业务成本', '损益', 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(48, 0, '5403', '营业税金及附加', '损益', 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(49, 0, '5601', '销售费用', '损益', 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(50, 0, '5602', '管理费用', '损益', 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(51, 0, '5603', '财务费用', '损益', 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(52, 0, '5711', '营业外支出', '损益', 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW()),
(53, 0, '5801', '所得税费用', '损益', 0, 1, 1, 0, 0, 0, 1, 1, 0, 0, 1, NOW(), 1, NOW());
