package com.company.daizhang.module.document.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 票据台账视图对象
 */
@Data
public class DocumentLedgerVO {

    /**
     * 票据总数
     */
    private Integer totalCount;

    /**
     * 已关联凭证数
     */
    private Integer linkedCount;

    /**
     * 未关联凭证数
     */
    private Integer unlinkedCount;

    /**
     * 金额合计
     */
    private BigDecimal totalAmount;

    /**
     * 按类型统计
     */
    private java.util.List<DocumentTypeStatVO> typeStats;

    /**
     * 按月统计
     */
    private java.util.List<DocumentMonthlyStatVO> monthlyStats;
}
