package com.company.daizhang.module.tax.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 申报表视图对象
 */
@Data
public class TaxDeclarationFormVO {

    /**
     * 申报表类型: VAT/Surcharge/IncomeTax/PersonalTax
     */
    private String formType;

    /**
     * 申报表名称
     */
    private String formName;

    /**
     * 纳税人名称
     */
    private String taxpayerName;

    /**
     * 纳税人识别号
     */
    private String taxNumber;

    /**
     * 年度
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 计税依据
     */
    private BigDecimal taxableIncome;

    /**
     * 税率
     */
    private BigDecimal taxRate;

    /**
     * 应纳税额
     */
    private BigDecimal taxAmount;

    /**
     * 申报表明细行
     */
    private List<TaxDeclarationFormItemVO> items;
}
