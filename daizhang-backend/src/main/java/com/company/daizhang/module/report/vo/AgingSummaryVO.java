package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 账龄分析汇总VO
 * <p>
 * 提供应收/应付总额、逾期金额及对应客户/供应商数量,用于报表顶部汇总卡片。
 */
@Data
public class AgingSummaryVO {

    /**
     * 应收总额(所有客户的未核销应收余额合计)
     */
    private BigDecimal totalReceivable;

    /**
     * 应付总额(所有供应商的未核销应付余额合计)
     */
    private BigDecimal totalPayable;

    /**
     * 逾期应收(账龄超过 30 天的应收金额合计,即 31 天以上分桶之和)
     */
    private BigDecimal overdueReceivable;

    /**
     * 逾期应付(账龄超过 30 天的应付金额合计,即 31 天以上分桶之和)
     */
    private BigDecimal overduePayable;

    /**
     * 有未核销应收余额的客户数
     */
    private Integer customerCount;

    /**
     * 有未核销应付余额的供应商数
     */
    private Integer supplierCount;
}
