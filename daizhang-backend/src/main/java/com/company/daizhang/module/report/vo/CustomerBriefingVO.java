package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 客户经营简报VO(B5)
 * 汇总单账套单月的关键经营指标，供代账公司给客户出具经营简报
 */
@Data
public class CustomerBriefingVO {

    private Long accountSetId;

    private String accountSetName;

    private Integer year;

    private Integer month;

    // ===== 经营概况 =====
    /**
     * 营业收入(本月)
     */
    private BigDecimal totalRevenue;

    /**
     * 营业成本+费用(本月)
     */
    private BigDecimal totalExpense;

    /**
     * 净利润(本月)
     */
    private BigDecimal netProfit;

    /**
     * 年累计收入
     */
    private BigDecimal yearAccumulatedRevenue;

    /**
     * 年累计净利润
     */
    private BigDecimal yearAccumulatedProfit;

    // ===== 资产概况 =====
    /**
     * 总资产
     */
    private BigDecimal totalAssets;

    /**
     * 总负债
     */
    private BigDecimal totalLiabilities;

    /**
     * 净资产(所有者权益)
     */
    private BigDecimal netAssets;

    /**
     * 资产负债率(%)
     */
    private BigDecimal debtRatio;

    // ===== 现金流概况 =====
    /**
     * 经营活动现金流净额
     */
    private BigDecimal operatingCashFlow;

    // ===== 税务概况 =====
    /**
     * 本月增值税
     */
    private BigDecimal vatAmount;

    /**
     * 本月企业所得税
     */
    private BigDecimal incomeTaxAmount;

    /**
     * 税负率(%)
     */
    private BigDecimal taxBurden;

    // ===== 关键指标 =====
    /**
     * 毛利率(%)
     */
    private BigDecimal grossMargin;

    /**
     * 净利率(%)
     */
    private BigDecimal netMargin;

    /**
     * 风险提示列表
     */
    private List<String> riskHints;
}
