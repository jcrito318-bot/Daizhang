-- ============================================
-- V7__add_tax_benchmark.sql - 税负基准表 + 行业数据
-- ============================================
-- 新增表:
--   1. tax_benchmark - 行业税负率基准表 (增值税/企业所得税基准+预警区间)
-- 种子数据:
--   - 11 条行业税负率基准 (含 DEFAULT 兜底行业)
--   数据来源: 参考国家税务总局发布的不同行业平均税负率
-- ============================================

CREATE TABLE `tax_benchmark` (
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

INSERT INTO `tax_benchmark` (`id`, `industry_code`, `industry_name`, `vat_benchmark_rate`, `vat_warning_low`, `vat_warning_high`, `eit_benchmark_rate`, `eit_warning_low`, `eit_warning_high`, `deleted`, `version`, `create_by`, `create_time`, `update_by`, `update_time`) VALUES
(1, 'A', '农林牧渔业', 0.0250, 0.0150, 0.0400, 0.0150, 0.0080, 0.0300, 0, 0, 1, NOW(), 1, NOW()),
(2, 'B', '采矿业', 0.0450, 0.0300, 0.0600, 0.0250, 0.0150, 0.0400, 0, 0, 1, NOW(), 1, NOW()),
(3, 'C', '制造业', 0.0350, 0.0250, 0.0500, 0.0200, 0.0120, 0.0350, 0, 0, 1, NOW(), 1, NOW()),
(4, 'F', '批发和零售业', 0.0200, 0.0120, 0.0350, 0.0150, 0.0080, 0.0250, 0, 0, 1, NOW(), 1, NOW()),
(5, 'G', '交通运输业', 0.0300, 0.0200, 0.0450, 0.0180, 0.0100, 0.0300, 0, 0, 1, NOW(), 1, NOW()),
(6, 'I', '信息技术服务业', 0.0400, 0.0250, 0.0600, 0.0250, 0.0150, 0.0400, 0, 0, 1, NOW(), 1, NOW()),
(7, 'K', '房地产业', 0.0500, 0.0350, 0.0700, 0.0400, 0.0250, 0.0600, 0, 0, 1, NOW(), 1, NOW()),
(8, 'L', '租赁和商务服务业', 0.0350, 0.0250, 0.0500, 0.0220, 0.0120, 0.0350, 0, 0, 1, NOW(), 1, NOW()),
(9, 'M', '科学研究和技术服务业', 0.0400, 0.0250, 0.0550, 0.0250, 0.0150, 0.0400, 0, 0, 1, NOW(), 1, NOW()),
(10, 'O', '居民服务和其他服务业', 0.0300, 0.0200, 0.0450, 0.0180, 0.0100, 0.0300, 0, 0, 1, NOW(), 1, NOW()),
(11, 'DEFAULT', '其他行业', 0.0300, 0.0200, 0.0450, 0.0200, 0.0120, 0.0350, 0, 0, 1, NOW(), 1, NOW());
