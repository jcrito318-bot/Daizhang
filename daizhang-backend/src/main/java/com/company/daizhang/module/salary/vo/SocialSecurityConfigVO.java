package com.company.daizhang.module.salary.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 社保公积金配置视图对象
 */
@Data
public class SocialSecurityConfigVO {

    private Long id;

    private Long accountSetId;

    private Integer year;

    private String city;

    private BigDecimal pensionEmployer;

    private BigDecimal pensionEmployee;

    private BigDecimal medicalEmployer;

    private BigDecimal medicalEmployee;

    private BigDecimal unemploymentEmployer;

    private BigDecimal unemploymentEmployee;

    private BigDecimal injuryEmployer;

    private BigDecimal maternityEmployer;

    private BigDecimal housingFundEmployer;

    private BigDecimal housingFundEmployee;

    private BigDecimal baseLower;

    private BigDecimal baseUpper;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
