package com.company.daizhang.module.dashboard.vo;

import lombok.Data;

import java.util.List;

/**
 * 仪表盘图表数据视图对象
 */
@Data
public class DashboardChartVO {

    /**
     * 收支趋势(近6月)
     */
    private List<ChartSeriesVO> revenueExpenseTrend;

    /**
     * 利润趋势
     */
    private List<ChartSeriesVO> profitTrend;

    /**
     * 凭证数趋势
     */
    private List<ChartSeriesVO> voucherCountTrend;

    /**
     * 资产分布
     */
    private List<ChartPieVO> assetDistribution;

    /**
     * 费用结构
     */
    private List<ChartPieVO> expenseStructure;

    /**
     * 税负趋势
     */
    private List<ChartSeriesVO> taxBurdenTrend;
}
