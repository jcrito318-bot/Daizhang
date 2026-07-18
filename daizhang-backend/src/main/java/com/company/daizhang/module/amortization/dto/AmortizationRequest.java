package com.company.daizhang.module.amortization.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 长期待摊费用请求
 */
@Data
public class AmortizationRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotBlank(message = "费用名称不能为空")
    private String amortizationName;

    private Long subjectId;

    @NotNull(message = "待摊总额不能为空")
    @DecimalMin(value = "0", message = "待摊总额不能为负数")
    @Digits(integer = 15, fraction = 2, message = "待摊总额精度超出范围")
    private BigDecimal totalAmount;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @NotNull(message = "总月数不能为空")
    @Min(value = 1, message = "摊销月数必须大于0")
    private Integer totalMonths;

    private String remark;
}
