package com.company.daizhang.module.salary.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 薪资表更新请求
 */
@Data
public class SalarySheetUpdateRequest {

    private BigDecimal baseSalary;

    private BigDecimal allowance;

    private BigDecimal bonus;

    private BigDecimal deduction;

    private BigDecimal socialSecurity;

    private BigDecimal housingFund;

    private String remark;
}
