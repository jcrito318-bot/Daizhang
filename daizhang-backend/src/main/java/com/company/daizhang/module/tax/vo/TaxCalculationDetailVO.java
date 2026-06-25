package com.company.daizhang.module.tax.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 税种计算明细视图对象
 */
@Data
public class TaxCalculationDetailVO {

    /**
     * 项目名称
     */
    private String itemName;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 税率
     */
    private BigDecimal rate;

    /**
     * 税额
     */
    private BigDecimal taxAmount;
}
