package com.company.daizhang.module.tax.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 行业税负率基准实体
 * <p>
 * 不同行业的增值税 / 企业所得税税负率基准及预警上下限。
 * 账套(AccSetSet.industryType)按行业代码(industry_code)匹配,
 * 未匹配到时回退到 industry_code='DEFAULT' 的基准。
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tax_benchmark")
public class TaxBenchmark extends BaseEntity {

    /**
     * 行业代码(国民经济行业分类门类代码:A/B/C/F/G/I/K/L/M/O/DEFAULT)
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
}
