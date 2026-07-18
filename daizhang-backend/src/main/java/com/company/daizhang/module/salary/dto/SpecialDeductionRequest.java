package com.company.daizhang.module.salary.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 个税专项附加扣除创建请求
 */
@Data
public class SpecialDeductionRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    private String employeeName;

    @NotBlank(message = "扣除项目类型不能为空")
    private String deductionType;

    private String deductionName;

    @NotNull(message = "月度金额不能为空")
    @DecimalMin(value = "0", message = "月度金额不能为负数")
    @Digits(integer = 12, fraction = 2, message = "月度金额精度超出范围")
    private BigDecimal monthlyAmount;

    @NotNull(message = "年度金额不能为空")
    @DecimalMin(value = "0", message = "年度金额不能为负数")
    @Digits(integer = 12, fraction = 2, message = "年度金额精度超出范围")
    private BigDecimal annualAmount;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private Integer status;

    private String remark;
}
