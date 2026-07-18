package com.company.daizhang.module.salary.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
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

    @DecimalMin(value = "0", message = "养老(企业)比例不能为负数")
    @DecimalMax(value = "1", message = "养老(企业)比例不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "养老(企业)比例精度超出范围")
    private BigDecimal pensionEmployer;

    @DecimalMin(value = "0", message = "养老(个人)比例不能为负数")
    @DecimalMax(value = "1", message = "养老(个人)比例不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "养老(个人)比例精度超出范围")
    private BigDecimal pensionEmployee;

    @DecimalMin(value = "0", message = "医疗(企业)比例不能为负数")
    @DecimalMax(value = "1", message = "医疗(企业)比例不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "医疗(企业)比例精度超出范围")
    private BigDecimal medicalEmployer;

    @DecimalMin(value = "0", message = "医疗(个人)比例不能为负数")
    @DecimalMax(value = "1", message = "医疗(个人)比例不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "医疗(个人)比例精度超出范围")
    private BigDecimal medicalEmployee;

    @DecimalMin(value = "0", message = "失业(企业)比例不能为负数")
    @DecimalMax(value = "1", message = "失业(企业)比例不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "失业(企业)比例精度超出范围")
    private BigDecimal unemploymentEmployer;

    @DecimalMin(value = "0", message = "失业(个人)比例不能为负数")
    @DecimalMax(value = "1", message = "失业(个人)比例不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "失业(个人)比例精度超出范围")
    private BigDecimal unemploymentEmployee;

    @DecimalMin(value = "0", message = "工伤(企业)比例不能为负数")
    @DecimalMax(value = "1", message = "工伤(企业)比例不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "工伤(企业)比例精度超出范围")
    private BigDecimal injuryEmployer;

    @DecimalMin(value = "0", message = "生育(企业)比例不能为负数")
    @DecimalMax(value = "1", message = "生育(企业)比例不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "生育(企业)比例精度超出范围")
    private BigDecimal maternityEmployer;

    @DecimalMin(value = "0", message = "公积金(企业)比例不能为负数")
    @DecimalMax(value = "1", message = "公积金(企业)比例不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "公积金(企业)比例精度超出范围")
    private BigDecimal housingFundEmployer;

    @DecimalMin(value = "0", message = "公积金(个人)比例不能为负数")
    @DecimalMax(value = "1", message = "公积金(个人)比例不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "公积金(个人)比例精度超出范围")
    private BigDecimal housingFundEmployee;

    @DecimalMin(value = "0", message = "缴费下限不能为负数")
    @Digits(integer = 10, fraction = 2, message = "缴费下限精度超出范围")
    private BigDecimal baseLower;

    @DecimalMin(value = "0", message = "缴费上限不能为负数")
    @Digits(integer = 10, fraction = 2, message = "缴费上限精度超出范围")
    private BigDecimal baseUpper;
}
