package com.company.daizhang.module.tax.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 税务计算更新请求
 */
@Data
public class TaxCalculationUpdateRequest {

    private String taxType;

    private String calculationItem;

    private BigDecimal amount;

    private BigDecimal rate;

    private BigDecimal taxAmount;

    private String remark;
}
