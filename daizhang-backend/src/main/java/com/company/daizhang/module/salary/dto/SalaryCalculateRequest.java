package com.company.daizhang.module.salary.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 薪资计算请求
 */
@Data
public class SalaryCalculateRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "年度不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;

    /**
     * 个税起征点，默认5000
     */
    @NotNull(message = "起征点不能为空")
    @DecimalMin(value = "0", message = "起征点不能为负数")
    @Digits(integer = 12, fraction = 2, message = "起征点精度超出范围")
    private BigDecimal threshold;
}
