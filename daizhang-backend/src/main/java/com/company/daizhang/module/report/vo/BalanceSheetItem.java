package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 资产负债表项目
 */
@Data
public class BalanceSheetItem {

    private Integer rowNo;

    private String name;

    private String code;

    private BigDecimal beginningBalance;

    private BigDecimal endingBalance;
}
