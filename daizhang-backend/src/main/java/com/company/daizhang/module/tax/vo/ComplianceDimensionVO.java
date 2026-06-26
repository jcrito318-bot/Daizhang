package com.company.daizhang.module.tax.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 合规评估维度VO
 */
@Data
public class ComplianceDimensionVO {

    /**
     * 维度编码：FINANCE(财务)/TAX(税务)/INVOICE(发票)/BUSINESS(经营)/OTHER(其他)
     */
    private String dimensionCode;

    /**
     * 维度名称
     */
    private String dimensionName;

    /**
     * 维度得分（0-100）
     */
    private BigDecimal score;

    /**
     * 该维度问题数
     */
    private Integer issueCount;

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
     * 该维度评估指标列表
     */
    private List<ComplianceIndicatorVO> indicators;
}
