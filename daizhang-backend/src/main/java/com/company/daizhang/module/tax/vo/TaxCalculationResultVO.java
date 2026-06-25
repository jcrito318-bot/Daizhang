package com.company.daizhang.module.tax.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 税种计算结果视图对象
 */
@Data
public class TaxCalculationResultVO {

    /**
     * 税种
     */
    private String taxType;

    /**
     * 税种名称
     */
    private String taxTypeName;

    /**
     * 计税依据
     */
    private BigDecimal taxableAmount;

    /**
     * 税率
     */
    private BigDecimal taxRate;

    /**
     * 应纳税额
     */
    private BigDecimal taxAmount;

    /**
     * 计算公式说明
     */
    private String calculationFormula;

    /**
     * 明细
     */
    private List<TaxCalculationDetailVO> details;
}
