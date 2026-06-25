package com.company.daizhang.module.salary.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 社保公积金配置请求
 */
@Data
public class SocialSecurityConfigRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "年度不能为空")
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
}
