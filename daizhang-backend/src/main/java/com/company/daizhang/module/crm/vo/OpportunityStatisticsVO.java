package com.company.daizhang.module.crm.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 商机统计视图对象
 */
@Data
public class OpportunityStatisticsVO {

    /**
     * 商机总数
     */
    private Long totalCount;

    /**
     * 线索数量
     */
    private Long clueCount;

    /**
     * 跟进数量
     */
    private Long followingCount;

    /**
     * 报价数量
     */
    private Long quotationCount;

    /**
     * 谈判数量
     */
    private Long negotiationCount;

    /**
     * 成交数量
     */
    private Long wonCount;

    /**
     * 流失数量
     */
    private Long lostCount;

    /**
     * 成交率（百分比）
     */
    private BigDecimal winRate;

    /**
     * 预计金额合计
     */
    private BigDecimal totalExpectedAmount;

    /**
     * 成交金额合计
     */
    private BigDecimal totalWonAmount;
}
