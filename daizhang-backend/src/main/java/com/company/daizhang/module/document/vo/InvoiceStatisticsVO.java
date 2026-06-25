package com.company.daizhang.module.document.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 发票统计视图对象
 */
@Data
public class InvoiceStatisticsVO {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 年度
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 进项发票数量
     */
    private Long inputInvoiceCount;

    /**
     * 已认证进项发票数量
     */
    private Long authenticatedInputCount;

    /**
     * 进项金额合计(不含税)
     */
    private BigDecimal inputAmount;

    /**
     * 进项税额合计
     */
    private BigDecimal inputTaxAmount;

    /**
     * 进项价税合计
     */
    private BigDecimal inputTotalAmount;

    /**
     * 销项发票数量
     */
    private Long outputInvoiceCount;

    /**
     * 正常销项发票数量
     */
    private Long normalOutputCount;

    /**
     * 销项金额合计(不含税)
     */
    private BigDecimal outputAmount;

    /**
     * 销项税额合计
     */
    private BigDecimal outputTaxAmount;

    /**
     * 销项价税合计
     */
    private BigDecimal outputTotalAmount;

    /**
     * 应纳增值税(销项税额-进项税额)
     */
    private BigDecimal vatPayable;
}
