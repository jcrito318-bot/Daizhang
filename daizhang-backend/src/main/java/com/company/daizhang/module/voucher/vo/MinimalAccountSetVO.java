package com.company.daizhang.module.voucher.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 极简账套VO
 * 用于识别零申报/无票客户
 */
@Data
public class MinimalAccountSetVO {

    private Long accountSetId;

    private String accountSetName;

    private String customerName;

    private Integer year;

    private Integer month;

    /**
     * 销项发票张数
     */
    private Integer outputInvoiceCount;

    /**
     * 进项发票张数
     */
    private Integer inputInvoiceCount;

    /**
     * 银行流水笔数
     */
    private Integer bankTransactionCount;

    /**
     * 已有凭证数
     */
    private Integer voucherCount;

    /**
     * 是否为零申报户（无销项/进项/银行流水）
     */
    private Boolean isZeroDeclaration;

    /**
     * 员工人数
     */
    private Integer employeeCount;

    /**
     * 月工资金额（预估）
     */
    private BigDecimal estimatedSalaryAmount;
}
