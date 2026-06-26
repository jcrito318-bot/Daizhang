package com.company.daizhang.module.tax.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 税务检查结果VO
 * 用于漏报/错报检查
 */
@Data
public class TaxCheckResultVO {

    private Long accountSetId;

    private String customerName;

    private Integer year;

    private Integer month;

    /**
     * 税种
     */
    private String taxType;

    /**
     * 检查类型: MISSING_DECLARATION(漏报)/AMOUNT_MISMATCH(错报金额)/STATUS_ABNORMAL(状态异常)
     */
    private String checkType;

    /**
     * 风险等级: 1-高 2-中 3-低
     */
    private Integer riskLevel;

    /**
     * 系统计算应纳税额
     */
    private BigDecimal expectedTaxAmount;

    /**
     * 申报的已纳税额
     */
    private BigDecimal declaredAmount;

    /**
     * 差异金额
     */
    private BigDecimal diffAmount;

    /**
     * 检查描述
     */
    private String description;

    /**
     * 建议
     */
    private String suggestion;
}
