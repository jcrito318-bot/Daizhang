package com.company.daizhang.module.customer.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 合同续费提醒视图对象
 */
@Data
public class ContractRenewalReminderVO {

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 客户ID
     */
    private Long customerId;

    /**
     * 客户名称
     */
    private String customerName;

    /**
     * 合同名称
     */
    private String contractName;

    /**
     * 结束日期
     */
    private LocalDate endDate;

    /**
     * 剩余天数
     */
    private Integer daysRemaining;

    /**
     * 合同金额
     */
    private BigDecimal contractAmount;

    /**
     * 状态: 即将到期/已到期
     */
    private String status;
}
