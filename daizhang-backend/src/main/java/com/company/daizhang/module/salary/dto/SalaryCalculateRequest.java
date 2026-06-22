package com.company.daizhang.module.salary.dto;

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
    private BigDecimal threshold;
}
