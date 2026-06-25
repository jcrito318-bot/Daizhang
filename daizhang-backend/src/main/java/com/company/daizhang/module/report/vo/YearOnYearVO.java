package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 同比环比分析VO
 */
@Data
public class YearOnYearVO {

    /**
     * 指标名
     */
    private String indicatorName;

    /**
     * 本期值
     */
    private BigDecimal currentValue;

    /**
     * 同期值(去年同期)
     */
    private BigDecimal previousValue;

    /**
     * 同比增长率
     */
    private BigDecimal yoyGrowthRate;

    /**
     * 上月值
     */
    private BigDecimal previousMonthValue;

    /**
     * 环比增长率
     */
    private BigDecimal momGrowthRate;
}
