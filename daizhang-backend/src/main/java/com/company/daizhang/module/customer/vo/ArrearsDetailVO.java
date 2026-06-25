package com.company.daizhang.module.customer.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 欠款明细视图对象
 */
@Data
public class ArrearsDetailVO {

    /**
     * 合同ID
     */
    private Long contractId;

    /**
     * 合同编号
     */
    private String contractNo;

    /**
     * 合同名称
     */
    private String contractName;

    /**
     * 合同金额
     */
    private BigDecimal contractAmount;

    /**
     * 已收款金额
     */
    private BigDecimal paidAmount;

    /**
     * 欠款金额
     */
    private BigDecimal arrearsAmount;

    /**
     * 合同到期日
     */
    private LocalDate endDate;

    /**
     * 逾期月数
     */
    private Integer overdueMonths;

    /**
     * 合同状态(0-草稿 1-执行中 2-已完成 3-已终止)
     */
    private Integer status;
}
