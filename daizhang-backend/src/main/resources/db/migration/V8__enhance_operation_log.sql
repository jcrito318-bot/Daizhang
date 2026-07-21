-- ============================================
-- V8__enhance_operation_log.sql - 操作日志字段扩展 (P3.4)
-- ============================================
-- 在 sys_operation_log 基础上扩展审计相关字段:
--   1. user_agent      - 客户端 User-Agent,用于追溯操作终端
--   2. before_value    - 操作前值(JSON),用于敏感操作前后对比
--   3. after_value     - 操作后值(JSON),用于敏感操作前后对比
--   4. request_path    - 请求路径(如 /system/user/1),便于按 URL 维度审计
--   5. request_method  - HTTP 方法(GET/POST/PUT/DELETE)
-- 注:ip 字段在 V1 baseline 已存在,此处不重复添加。
-- ============================================

ALTER TABLE `sys_operation_log` ADD COLUMN `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '客户端 User-Agent';
ALTER TABLE `sys_operation_log` ADD COLUMN `before_value` TEXT DEFAULT NULL COMMENT '操作前值(JSON)';
ALTER TABLE `sys_operation_log` ADD COLUMN `after_value` TEXT DEFAULT NULL COMMENT '操作后值(JSON)';
ALTER TABLE `sys_operation_log` ADD COLUMN `request_path` VARCHAR(500) DEFAULT NULL COMMENT '请求路径';
ALTER TABLE `sys_operation_log` ADD COLUMN `request_method` VARCHAR(10) DEFAULT NULL COMMENT 'HTTP 方法';

-- 按请求路径与创建时间建索引,便于按 URL 维度查询历史操作
CREATE INDEX `idx_request_path` ON `sys_operation_log` (`request_path`);
