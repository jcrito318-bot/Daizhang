package com.company.daizhang.module.customer.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 欠款视图对象
 */
@Data
public class ArrearsVO {

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 合同总额
     */
    private BigDecimal totalContractAmount;

    /**
     * 已收款
     */
    private BigDecimal totalPaidAmount;

    /**
     * 欠款总额
     */
    private BigDecimal totalArrearsAmount;

    /**
     * 逾期月数
     */
    private Integer overdueMonths;

    /**
     * 风险等级
     */
    private String riskLevel;

    /**
     * 欠款明细
     */
    private List<ArrearsDetailVO> details;
}
