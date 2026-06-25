package com.company.daizhang.module.amortization.dto;

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
    private BigDecimal totalAmount;

    @NotNull(message = "开始日期不能为空")
    private LocalDate startDate;

    @NotNull(message = "结束日期不能为空")
    private LocalDate endDate;

    @NotNull(message = "总月数不能为空")
    private Integer totalMonths;

    private String remark;
}
