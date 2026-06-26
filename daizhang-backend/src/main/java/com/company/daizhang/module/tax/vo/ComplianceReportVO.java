package com.company.daizhang.module.tax.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 财税合规评估报告VO
 * 基于5大维度、30+指标的企业财税合规风险评估
 */
@Data
public class ComplianceReportVO {

    private Long accountSetId;

    private String customerName;

    private Integer year;

    private Integer month;

    /**
     * 总体合规分（0-100）
     */
    private BigDecimal overallScore;

    /**
     * 风险等级：低风险/中风险/高风险
     */
    private String overallRiskLevel;

    /**
     * 问题总数
     */
    private Integer totalIssueCount;

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
     * 五大维度评估结果
     */
    private List<ComplianceDimensionVO> dimensions;

    /**
     * 问题明细列表
     */
    private List<ComplianceIssueVO> issues;

    /**
     * 总体建议
     */
    private String overallSuggestion;

    /**
     * 生成时间
     */
    private String generateTime;
}
