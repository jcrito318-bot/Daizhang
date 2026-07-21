package com.company.daizhang.module.tax.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 行业税负率基准视图对象
 */
@Data
public class TaxBenchmarkVO {

    private Long id;

    /**
     * 行业代码
     */
    private String industryCode;

    /**
     * 行业名称
     */
    private String industryName;

    /**
     * 增值税税负率基准(0.0300 = 3.00%)
     */
    private BigDecimal vatBenchmarkRate;

    /**
     * 增值税税负率下限预警
     */
    private BigDecimal vatWarningLow;

    /**
     * 增值税税负率上限预警
     */
    private BigDecimal vatWarningHigh;

    /**
     * 企业所得税税负率基准
     */
    private BigDecimal eitBenchmarkRate;

    /**
     * 企业所得税税负率下限预警
     */
    private BigDecimal eitWarningLow;

    /**
     * 企业所得税税负率上限预警
     */
    private BigDecimal eitWarningHigh;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
