package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 现金流量表调整VO
 */
@Data
public class CashFlowAdjustmentVO {

    private Long id;

    private Long accountSetId;

    private Integer year;

    private Integer month;

    /**
     * 调整项名称
     */
    private String itemName;

    /**
     * 经营/投资/筹资
     */
    private String category;

    /**
     * 原始金额
     */
    private BigDecimal originalAmount;

    /**
     * 调整后金额
     */
    private BigDecimal adjustedAmount;

    /**
     * 调整原因
     */
    private String adjustmentReason;
}
