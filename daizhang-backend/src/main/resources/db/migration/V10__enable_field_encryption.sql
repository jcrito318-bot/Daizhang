-- ============================================
-- V10__enable_field_encryption.sql - 字段加密功能启用 (P4.1)
-- ============================================
-- 本迁移完成两件事:
--   1. 加宽敏感字段列宽:AES-GCM 密文为 base64(IV(12B) + ciphertext + tag(16B)),
--      长度显著大于明文(如 18 位身份证 → 约 64 字符密文),原 VARCHAR(20/50) 会被截断。
--      统一加宽到 VARCHAR(512),足够容纳任意合理明文加密后的密文。
--   2. 记录加密版本号。
--
-- 注意:已有明文数据的加密迁移由应用启动时自动处理(FieldEncryptionMigrationRunner),
--      扫描标注 @FieldEncrypt 的字段,若内容不是 base64 密文格式则批量加密更新。
--      迁移失败不阻塞应用启动(仅记录 WARN 日志)。

-- 员工表:身份证号 / 联系电话 / 银行账号
ALTER TABLE `sal_employee` ALTER COLUMN `id_card` VARCHAR(512) DEFAULT NULL;
ALTER TABLE `sal_employee` ALTER COLUMN `phone` VARCHAR(512) DEFAULT NULL;
ALTER TABLE `sal_employee` ALTER COLUMN `bank_account` VARCHAR(512) DEFAULT NULL;

-- 系统用户表:手机号
ALTER TABLE `sys_user` ALTER COLUMN `phone` VARCHAR(512) DEFAULT NULL;

-- 客户表:联系电话 / 银行账号
ALTER TABLE `cst_customer` ALTER COLUMN `contact_phone` VARCHAR(512) DEFAULT NULL;
ALTER TABLE `cst_customer` ALTER COLUMN `bank_account` VARCHAR(512) DEFAULT NULL;

-- 银行流水表:银行账号
ALTER TABLE `bank_transaction` ALTER COLUMN `bank_account` VARCHAR(512) NOT NULL;

-- 银行账户主数据表:银行账号
ALTER TABLE `bank_account` ALTER COLUMN `account_number` VARCHAR(512) NOT NULL;
