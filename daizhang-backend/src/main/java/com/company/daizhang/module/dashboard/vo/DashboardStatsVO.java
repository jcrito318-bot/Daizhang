package com.company.daizhang.module.dashboard.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 仪表盘统计数据
 */
@Data
public class DashboardStatsVO {
    
    private Long accountSetCount;
    
    private Long monthVoucherCount;
    
    private Long pendingAuditCount;
    
    private Long pendingTaxCount;
    
    private BigDecimal totalAssets;
    
    private BigDecimal totalRevenue;
    
    private BigDecimal totalProfit;
    
    private BigDecimal cashBalance;
}
