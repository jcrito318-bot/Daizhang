package com.company.daizhang.module.salary.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 社保计算结果视图对象
 */
@Data
public class SocialSecurityCalculationVO {

    /**
     * 缴费基数（受上下限限制后）
     */
    private BigDecimal base;

    /**
     * 养老单位部分
     */
    private BigDecimal pensionEmployer;

    /**
     * 养老个人部分
     */
    private BigDecimal pensionEmployee;

    /**
     * 医疗单位部分
     */
    private BigDecimal medicalEmployer;

    /**
     * 医疗个人部分
     */
    private BigDecimal medicalEmployee;

    /**
     * 失业单位部分
     */
    private BigDecimal unemploymentEmployer;

    /**
     * 失业个人部分
     */
    private BigDecimal unemploymentEmployee;

    /**
     * 工伤单位部分
     */
    private BigDecimal injuryEmployer;

    /**
     * 生育单位部分
     */
    private BigDecimal maternityEmployer;

    /**
     * 公积金单位部分
     */
    private BigDecimal housingFundEmployer;

    /**
     * 公积金个人部分
     */
    private BigDecimal housingFundEmployee;

    /**
     * 单位部分合计
     */
    private BigDecimal employerTotal;

    /**
     * 个人部分合计
     */
    private BigDecimal employeeTotal;

    /**
     * 总计
     */
    private BigDecimal total;
}
