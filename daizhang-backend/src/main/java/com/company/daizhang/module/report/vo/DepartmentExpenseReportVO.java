package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 部门费用分析表VO
 */
@Data
public class DepartmentExpenseReportVO {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 年
     */
    private Integer year;

    /**
     * 月
     */
    private Integer month;

    /**
     * 费用科目合计（本期）
     */
    private BigDecimal totalExpense;

    /**
     * 部门费用明细列表
     */
    private List<DepartmentExpenseItem> items;
}
