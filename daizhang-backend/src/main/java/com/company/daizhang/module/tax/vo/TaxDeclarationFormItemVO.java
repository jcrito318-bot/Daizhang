package com.company.daizhang.module.tax.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 申报表明细行视图对象
 */
@Data
public class TaxDeclarationFormItemVO {

    /**
     * 行次
     */
    private Integer rowNo;

    /**
     * 项目名称
     */
    private String itemName;

    /**
     * 公式
     */
    private String formula;

    /**
     * 金额
     */
    private BigDecimal amount;
}
