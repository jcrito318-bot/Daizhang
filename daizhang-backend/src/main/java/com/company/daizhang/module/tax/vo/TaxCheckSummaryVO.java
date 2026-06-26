package com.company.daizhang.module.tax.vo;

import lombok.Data;

import java.util.List;

/**
 * 税务检查汇总VO
 */
@Data
public class TaxCheckSummaryVO {

    private Integer year;

    private Integer month;

    /**
     * 检查的账套数量
     */
    private Integer totalAccountSets;

    /**
     * 发现问题的账套数量
     */
    private Integer problemCount;

    /**
     * 漏报数量
     */
    private Integer missingDeclarationCount;

    /**
     * 错报数量
     */
    private Integer amountMismatchCount;

    /**
     * 高风险问题数
     */
    private Integer highRiskCount;

    /**
     * 中风险问题数
     */
    private Integer mediumRiskCount;

    /**
     * 低风险问题数
     */
    private Integer lowRiskCount;

    /**
     * 检查结果明细
     */
    private List<TaxCheckResultVO> results;
}
