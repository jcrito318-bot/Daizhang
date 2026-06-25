package com.company.daizhang.module.customer.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户服务报告视图对象
 */
@Data
public class ServiceReportVO {

    private Long id;

    private Long accountSetId;

    private Long customerId;

    private Integer reportYear;

    private Integer reportMonth;

    private String reportType;

    private BigDecimal totalRevenue;

    private BigDecimal totalExpense;

    private BigDecimal netProfit;

    private BigDecimal taxAmount;

    private String financialSummary;

    private String riskWarning;

    private String suggestion;

    private Integer status;

    private Long createBy;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
