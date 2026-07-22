-- ============================================
-- V15__add_payslip_push_record.sql - 工资条推送记录表 (B7)
-- ============================================
-- 记录每次工资条推送的明细,供推送管理界面查询/重推使用。
-- 字段说明:
--   salary_sheet_id  - 薪资表ID(关联 sal_salary_sheet)
--   employee_id      - 员工ID
--   employee_name    - 员工姓名(冗余,便于展示)
--   account_set_id   - 账套ID
--   year/month       - 薪资所属年月
--   push_method      - 推送方式: PDF/EMAIL/SMS
--   push_status      - 推送状态: 0-待推送 1-已推送 2-失败
--   push_time        - 推送时间
--   file_path        - 生成的PDF文件路径
--   error_message    - 失败原因
--   deleted          - 逻辑删除标志
--   version          - 乐观锁
-- ============================================

CREATE TABLE IF NOT EXISTS `salary_payslip_push` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `salary_sheet_id` BIGINT NOT NULL COMMENT '薪资表ID',
  `employee_id` BIGINT NOT NULL COMMENT '员工ID',
  `employee_name` VARCHAR(100) COMMENT '员工姓名',
  `account_set_id` BIGINT COMMENT '账套ID',
  `year` INT COMMENT '年份',
  `month` INT COMMENT '月份',
  `push_method` VARCHAR(20) DEFAULT 'PDF' COMMENT '推送方式: PDF/EMAIL/SMS',
  `push_status` INT DEFAULT 0 COMMENT '推送状态: 0-待推送 1-已推送 2-失败',
  `push_time` DATETIME COMMENT '推送时间',
  `file_path` VARCHAR(500) COMMENT '生成的PDF文件路径',
  `error_message` VARCHAR(500) COMMENT '失败原因',
  `create_by` BIGINT COMMENT '创建人',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` BIGINT COMMENT '更新人',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` INT DEFAULT 0 COMMENT '逻辑删除',
  `version` INT DEFAULT 0 COMMENT '乐观锁',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工资条推送记录表';

CREATE INDEX IF NOT EXISTS `idx_payslip_push_sheet` ON `salary_payslip_push`(`salary_sheet_id`);
CREATE INDEX IF NOT EXISTS `idx_payslip_push_employee` ON `salary_payslip_push`(`employee_id`);
