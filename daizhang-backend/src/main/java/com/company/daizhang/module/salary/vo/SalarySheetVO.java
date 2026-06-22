package com.company.daizhang.module.salary.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 薪资表视图对象
 */
@Data
public class SalarySheetVO {

    private Long id;

    private Long accountSetId;

    private Integer year;

    private Integer month;

    private Long employeeId;

    private String employeeName;

    private BigDecimal baseSalary;

    private BigDecimal allowance;

    private BigDecimal bonus;

    private BigDecimal deduction;

    private BigDecimal socialSecurity;

    private BigDecimal housingFund;

    private BigDecimal taxableIncome;

    private BigDecimal incomeTax;

    private BigDecimal netSalary;

    private Integer status;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;

    private String createByName;
}
