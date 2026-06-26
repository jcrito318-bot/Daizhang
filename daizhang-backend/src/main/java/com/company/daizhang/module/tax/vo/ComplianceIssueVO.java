package com.company.daizhang.module.tax.vo;

import lombok.Data;

/**
 * 合规问题VO
 */
@Data
public class ComplianceIssueVO {

    /**
     * 所属维度
     */
    private String dimensionCode;

    /**
     * 问题编码
     */
    private String issueCode;

    /**
     * 问题标题
     */
    private String issueTitle;

    /**
     * 风险等级：1-高 2-中 3-低
     */
    private Integer riskLevel;

    /**
     * 问题描述
     */
    private String description;

    /**
     * 风险影响
     */
    private String riskImpact;

    /**
     * 整改建议
     */
    private String suggestion;

    /**
     * 涉及金额
     */
    private java.math.BigDecimal involvedAmount;
}
