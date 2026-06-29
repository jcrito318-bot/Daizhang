package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 利润表VO
 */
@Data
public class IncomeStatementVO {

    private List<IncomeStatementItem> items;

    private BigDecimal totalRevenue;

    private BigDecimal totalExpense;

    private BigDecimal netProfit;

    // 本年累计金额(1月~当前月份)
    // 原实现仅计算未输出,导致前端无法展示本年累计净利润等关键指标
    private BigDecimal totalRevenueYear;

    private BigDecimal totalExpenseYear;

    private BigDecimal totalProfit;

    private BigDecimal totalProfitYear;

    private BigDecimal netProfitYear;
}
