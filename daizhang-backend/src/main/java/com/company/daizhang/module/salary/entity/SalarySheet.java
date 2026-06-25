package com.company.daizhang.module.salary.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 薪资表实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sal_salary_sheet")
public class SalarySheet extends BaseEntity {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 年度
     */
    @TableField("`year`")
    private Integer year;

    /**
     * 月份
     */
    @TableField("`month`")
    private Integer month;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 基本工资
     */
    private BigDecimal baseSalary;

    /**
     * 津贴补贴
     */
    private BigDecimal allowance;

    /**
     * 奖金
     */
    private BigDecimal bonus;

    /**
     * 扣款
     */
    private BigDecimal deduction;

    /**
     * 社保
     */
    private BigDecimal socialSecurity;

    /**
     * 公积金
     */
    private BigDecimal housingFund;

    /**
     * 应纳税所得额
     */
    private BigDecimal taxableIncome;

    /**
     * 个人所得税
     */
    private BigDecimal incomeTax;

    /**
     * 实发工资
     */
    private BigDecimal netSalary;

    /**
     * 状态：0-草稿 1-已确认 2-已发放
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
