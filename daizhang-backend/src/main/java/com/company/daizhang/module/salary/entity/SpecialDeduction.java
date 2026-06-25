package com.company.daizhang.module.salary.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 个税专项附加扣除实体
 * 扣除项目类型：子女教育、继续教育、大病医疗、住房贷款利息、住房租金、赡养老人、3岁以下婴幼儿照护
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sal_special_deduction")
public class SpecialDeduction extends BaseEntity {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 员工姓名（冗余字段，便于查询）
     */
    private String employeeName;

    /**
     * 扣除项目类型：
     * CHILDREN_EDUCATION-子女教育
     * CONTINUING_EDUCATION-继续教育
     * SERIOUS_ILLNESS-大病医疗
     * HOUSING_LOAN-住房贷款利息
     * HOUSING_RENT-住房租金
     * SUPPORT_ELDERLY-赡养老人
     * INFANT_CARE-3岁以下婴幼儿照护
     */
    private String deductionType;

    /**
     * 扣除项目名称
     */
    private String deductionName;

    /**
     * 月度扣除标准金额（元）
     */
    private BigDecimal monthlyAmount;

    /**
     * 年度扣除金额（元，仅大病医疗按年度累计）
     */
    private BigDecimal annualAmount;

    /**
     * 有效起始月份 (yyyy-MM)
     */
    private LocalDate effectiveFrom;

    /**
     * 有效截止月份 (yyyy-MM)
     */
    private LocalDate effectiveTo;

    /**
     * 状态 0-停用 1-生效中
     */
    private Integer status;

    /**
     * 备注
     */
    private String remark;
}
