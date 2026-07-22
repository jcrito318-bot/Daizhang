package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 多年度对比分析VO(B6)
 * 跨年度财务数据对比，含同比增长率
 */
@Data
public class MultiYearComparisonVO {

    private Long accountSetId;

    private Integer startYear;

    private Integer endYear;

    /**
     * 每年一行
     */
    private List<YearlyData> years;

    /**
     * 单年度数据
     */
    @Data
    public static class YearlyData {

        private Integer year;

        /**
         * 年营业收入
         */
        private BigDecimal totalRevenue;

        /**
         * 年营业成本+费用
         */
        private BigDecimal totalExpense;

        /**
         * 年净利润
         */
        private BigDecimal netProfit;

        /**
         * 年末总资产
         */
        private BigDecimal totalAssets;

        /**
         * 年末总负债
         */
        private BigDecimal totalLiabilities;

        /**
         * 年末净资产
         */
        private BigDecimal netAssets;

        /**
         * 年经营活动现金流
         */
        private BigDecimal operatingCashFlow;

        /**
         * 年增值税合计
         */
        private BigDecimal vatAmount;

        /**
         * 年税负率(%)
         */
        private BigDecimal taxBurden;

        // ===== 同比增长率(第一年为null) =====
        /**
         * 收入增长率(%)
         */
        private BigDecimal revenueGrowthRate;

        /**
         * 利润增长率(%)
         */
        private BigDecimal profitGrowthRate;

        /**
         * 资产增长率(%)
         */
        private BigDecimal assetGrowthRate;
    }
}
