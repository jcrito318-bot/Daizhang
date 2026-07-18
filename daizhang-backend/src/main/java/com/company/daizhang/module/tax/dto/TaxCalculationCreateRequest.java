package com.company.daizhang.module.tax.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 税务计算创建请求
 */
@Data
public class TaxCalculationCreateRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "年度不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;

    @NotBlank(message = "税种不能为空")
    private String taxType;

    @NotBlank(message = "计算项目不能为空")
    private String calculationItem;

    @NotNull(message = "计税金额不能为空")
    @DecimalMin(value = "0", message = "计税金额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "计税金额精度超出范围")
    private BigDecimal amount;

    @NotNull(message = "税率不能为空")
    @DecimalMin(value = "0", message = "税率不能为负数")
    @DecimalMax(value = "1", message = "税率不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "税率精度超出范围")
    private BigDecimal rate;

    @NotNull(message = "税额不能为空")
    @DecimalMin(value = "0", message = "税额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "税额精度超出范围")
    private BigDecimal taxAmount;

    private String remark;
}
