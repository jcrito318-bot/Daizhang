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
}
