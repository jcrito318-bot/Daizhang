package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 利润表项目
 */
@Data
public class IncomeStatementItem {

    private Integer rowNo;

    private String name;

    private String code;

    private BigDecimal currentAmount;

    private BigDecimal yearAmount;
}
