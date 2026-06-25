package com.company.daizhang.module.customer.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 客户服务报告实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("cst_service_report")
public class ServiceReport extends BaseEntity {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 报告年度
     */
    private Integer reportYear;

    /**
     * 报告月份(空=年度报告)
     */
    private Integer reportMonth;

    /**
     * 报告类型:月度/季度/年度
     */
    private String reportType;

    /**
     * 总收入
     */
    private BigDecimal totalRevenue;

    /**
     * 总支出
     */
    private BigDecimal totalExpense;

    /**
     * 净利润
     */
    private BigDecimal netProfit;

    /**
     * 纳税总额
     */
    private BigDecimal taxAmount;

    /**
     * 财务摘要
     */
    private String financialSummary;

    /**
     * 风险提示
     */
    private String riskWarning;

    /**
     * 经营建议
     */
    private String suggestion;

    /**
     * 状态 0-草稿 1-已发布
     */
    private Integer status;
}
