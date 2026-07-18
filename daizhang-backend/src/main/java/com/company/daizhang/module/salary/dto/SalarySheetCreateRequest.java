package com.company.daizhang.module.salary.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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

    @DecimalMin(value = "0", message = "基本工资不能为负数")
    @Digits(integer = 12, fraction = 2, message = "基本工资精度超出范围")
    private BigDecimal baseSalary;

    @DecimalMin(value = "0", message = "津贴不能为负数")
    @Digits(integer = 12, fraction = 2, message = "津贴精度超出范围")
    private BigDecimal allowance;

    @DecimalMin(value = "0", message = "奖金不能为负数")
    @Digits(integer = 12, fraction = 2, message = "奖金精度超出范围")
    private BigDecimal bonus;

    @DecimalMin(value = "0", message = "扣款不能为负数")
    @Digits(integer = 12, fraction = 2, message = "扣款精度超出范围")
    private BigDecimal deduction;

    @DecimalMin(value = "0", message = "社保不能为负数")
    @Digits(integer = 12, fraction = 2, message = "社保精度超出范围")
    private BigDecimal socialSecurity;

    @DecimalMin(value = "0", message = "公积金不能为负数")
    @Digits(integer = 12, fraction = 2, message = "公积金精度超出范围")
    private BigDecimal housingFund;

    private String remark;
}
