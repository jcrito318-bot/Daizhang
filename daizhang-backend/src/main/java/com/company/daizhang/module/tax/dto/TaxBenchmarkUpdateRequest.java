package com.company.daizhang.module.tax.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 行业税负率基准更新请求(ADMIN only)
 */
@Data
public class TaxBenchmarkUpdateRequest {

    @NotNull(message = "增值税税负率基准不能为空")
    @DecimalMin(value = "0", message = "增值税税负率基准不能为负数")
    @DecimalMax(value = "1", message = "增值税税负率基准不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "增值税税负率基准精度超出范围")
    private BigDecimal vatBenchmarkRate;

    @NotNull(message = "增值税税负率下限预警不能为空")
    @DecimalMin(value = "0", message = "增值税税负率下限预警不能为负数")
    @DecimalMax(value = "1", message = "增值税税负率下限预警不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "增值税税负率下限预警精度超出范围")
    private BigDecimal vatWarningLow;

    @NotNull(message = "增值税税负率上限预警不能为空")
    @DecimalMin(value = "0", message = "增值税税负率上限预警不能为负数")
    @DecimalMax(value = "1", message = "增值税税负率上限预警不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "增值税税负率上限预警精度超出范围")
    private BigDecimal vatWarningHigh;

    @NotNull(message = "企业所得税税负率基准不能为空")
    @DecimalMin(value = "0", message = "企业所得税税负率基准不能为负数")
    @DecimalMax(value = "1", message = "企业所得税税负率基准不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "企业所得税税负率基准精度超出范围")
    private BigDecimal eitBenchmarkRate;

    @NotNull(message = "企业所得税税负率下限预警不能为空")
    @DecimalMin(value = "0", message = "企业所得税税负率下限预警不能为负数")
    @DecimalMax(value = "1", message = "企业所得税税负率下限预警不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "企业所得税税负率下限预警精度超出范围")
    private BigDecimal eitWarningLow;

    @NotNull(message = "企业所得税税负率上限预警不能为空")
    @DecimalMin(value = "0", message = "企业所得税税负率上限预警不能为负数")
    @DecimalMax(value = "1", message = "企业所得税税负率上限预警不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "企业所得税税负率上限预警精度超出范围")
    private BigDecimal eitWarningHigh;
}
