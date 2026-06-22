package com.company.daizhang.module.tax.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 税务申报更新请求
 */
@Data
public class TaxDeclarationUpdateRequest {

    private String taxType;

    private BigDecimal taxableAmount;

    private BigDecimal taxRate;

    private BigDecimal taxAmount;

    private BigDecimal declaredAmount;

    private BigDecimal actualAmount;

    private Integer status;

    private LocalDate declarationDate;

    private LocalDate paymentDate;

    private String remark;
}
