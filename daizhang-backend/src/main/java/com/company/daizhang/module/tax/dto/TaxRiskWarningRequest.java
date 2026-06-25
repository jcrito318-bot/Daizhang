package com.company.daizhang.module.tax.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 税务风险预警创建请求
 */
@Data
public class TaxRiskWarningRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotNull(message = "年度不能为空")
    private Integer year;

    @NotNull(message = "月份不能为空")
    private Integer month;

    @NotBlank(message = "风险类型不能为空")
    private String riskType;

    private Integer riskLevel;

    @NotBlank(message = "风险描述不能为空")
    private String riskDescription;

    private String riskValue;

    private String suggestion;
}
