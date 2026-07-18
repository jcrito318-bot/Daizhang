package com.company.daizhang.module.tax.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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

    @NotNull(message = "计税金额不能为空")
    @DecimalMin(value = "0", message = "计税金额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "计税金额精度超出范围")
    private BigDecimal taxableAmount;

    @NotNull(message = "税率不能为空")
    @DecimalMin(value = "0", message = "税率不能为负数")
    @DecimalMax(value = "1", message = "税率不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "税率精度超出范围")
    private BigDecimal taxRate;

    @NotNull(message = "税额不能为空")
    @DecimalMin(value = "0", message = "税额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "税额精度超出范围")
    private BigDecimal taxAmount;

    @NotNull(message = "申报金额不能为空")
    @DecimalMin(value = "0", message = "申报金额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "申报金额精度超出范围")
    private BigDecimal declaredAmount;

    @NotNull(message = "实际金额不能为空")
    @DecimalMin(value = "0", message = "实际金额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "实际金额精度超出范围")
    private BigDecimal actualAmount;

    private LocalDate declarationDate;

    private LocalDate paymentDate;

    private String remark;
}
