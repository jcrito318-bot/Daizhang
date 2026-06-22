package com.company.daizhang.module.tax.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 税务申报创建请求
 */
@Data
public class TaxDeclarationCreateRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "年度不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;

    @NotBlank(message = "税种不能为空")
    private String taxType;

    private BigDecimal taxableAmount;

    private BigDecimal taxRate;

    private BigDecimal taxAmount;

    private BigDecimal declaredAmount;

    private BigDecimal actualAmount;

    private LocalDate declarationDate;

    private LocalDate paymentDate;

    private String remark;
}
