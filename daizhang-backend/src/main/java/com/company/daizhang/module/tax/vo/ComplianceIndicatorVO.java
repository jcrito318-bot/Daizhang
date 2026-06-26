package com.company.daizhang.module.tax.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 合规评估指标VO
 */
@Data
public class ComplianceIndicatorVO {

    /**
     * 指标编码
     */
    private String indicatorCode;

    /**
     * 指标名称
     */
    private String indicatorName;

    /**
     * 指标值
     */
    private BigDecimal indicatorValue;

    /**
     * 指标单位
     */
    private String unit;

    /**
     * 参考值范围
     */
    private String referenceRange;

    /**
     * 状态：NORMAL(正常)/WARNING(预警)/ABNORMAL(异常)
     */
    private String status;

    /**
     * 说明
     */
    private String description;
}
