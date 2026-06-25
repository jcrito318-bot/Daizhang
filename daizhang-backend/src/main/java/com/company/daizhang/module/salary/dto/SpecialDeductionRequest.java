package com.company.daizhang.module.salary.dto;

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

    private BigDecimal monthlyAmount;

    private BigDecimal annualAmount;

    private LocalDate effectiveFrom;

    private LocalDate effectiveTo;

    private Integer status;

    private String remark;
}
