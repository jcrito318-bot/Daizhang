package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 部门费用分析项VO
 */
@Data
public class DepartmentExpenseItem {

    /**
     * 部门ID
     */
    private Long departmentId;

    /**
     * 部门编码
     */
    private String departmentCode;

    /**
     * 部门名称
     */
    private String departmentName;

    /**
     * 本期借方发生额（费用合计）
     */
    private BigDecimal periodAmount;

    /**
     * 本年累计借方发生额
     */
    private BigDecimal yearAmount;

    /**
     * 占比（%）
     */
    private BigDecimal percentage;
}
