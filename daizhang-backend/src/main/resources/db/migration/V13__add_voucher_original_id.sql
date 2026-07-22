-- ============================================
-- V13__add_voucher_original_id.sql - 凭证红冲关联字段 (P5.2.1)
-- ============================================
-- 新增 acc_voucher.original_voucher_id 字段:
--   红冲关联的原凭证ID,null 表示非红冲凭证。
--   配合 VoucherServiceImpl.reverseVoucher 在创建红冲凭证时回填原凭证ID,
--   替代此前仅靠摘要软关联的脆弱方案,支撑"是否已被红冲"校验。
-- ============================================

ALTER TABLE `acc_voucher` ADD COLUMN `original_voucher_id` BIGINT DEFAULT NULL COMMENT '红冲关联的原凭证ID';

CREATE INDEX IF NOT EXISTS `idx_voucher_original_id` ON `acc_voucher`(`original_voucher_id`);
