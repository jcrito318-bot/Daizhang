package com.company.daizhang.module.salary.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 薪资表创建请求
 */
@Data
public class SalarySheetCreateRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "年度不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;

    @NotNull(message = "员工ID不能为空")
    private Long employeeId;

    private BigDecimal baseSalary;

    private BigDecimal allowance;

    private BigDecimal bonus;

    private BigDecimal deduction;

    private BigDecimal socialSecurity;

    private BigDecimal housingFund;

    private String remark;
}
