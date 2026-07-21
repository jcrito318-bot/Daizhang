package com.company.daizhang.module.tax.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 税负率趋势视图对象(全年趋势分析用)
 */
@Data
public class TaxTrendVO {

    /**
     * 年度
     */
    private Integer year;

    /**
     * 月份(1-12)
     */
    private Integer month;

    /**
     * 增值税实际税负率(保留4位小数;无数据时为 null)
     */
    private BigDecimal vatRate;

    /**
     * 企业所得税实际税负率(保留4位小数;无数据时为 null)
     */
    private BigDecimal eitRate;
}
