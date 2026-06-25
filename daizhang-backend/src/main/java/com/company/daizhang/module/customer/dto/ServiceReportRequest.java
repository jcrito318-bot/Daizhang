package com.company.daizhang.module.customer.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 客户服务报告创建/更新请求
 */
@Data
public class ServiceReportRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    private Long customerId;

    @NotNull(message = "报告年度不能为空")
    private Integer reportYear;

    private Integer reportMonth;

    @NotNull(message = "报告类型不能为空")
    private String reportType;

    private BigDecimal totalRevenue;

    private BigDecimal totalExpense;

    private BigDecimal netProfit;

    private BigDecimal taxAmount;

    private String financialSummary;

    private String riskWarning;

    private String suggestion;

    private Integer status;
}
