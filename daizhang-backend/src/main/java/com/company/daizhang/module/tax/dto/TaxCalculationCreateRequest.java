package com.company.daizhang.module.tax.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 税务计算创建请求
 */
@Data
public class TaxCalculationCreateRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "年度不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;

    @NotBlank(message = "税种不能为空")
    private String taxType;

    @NotBlank(message = "计算项目不能为空")
    private String calculationItem;

    private BigDecimal amount;

    private BigDecimal rate;

    private BigDecimal taxAmount;

    private String remark;
}
