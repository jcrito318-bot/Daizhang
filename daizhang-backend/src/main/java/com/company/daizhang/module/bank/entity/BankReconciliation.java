package com.company.daizhang.module.bank.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 银行对账结果实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bank_reconciliation")
public class BankReconciliation extends BaseEntity {

    private Long accountSetId;

    private String bankAccount;

    private Integer year;

    private Integer month;

    private BigDecimal bankBalance;

    private BigDecimal bookBalance;

    /**
     * 未达账项数量
     */
    private Integer unreconciledItems;

    private LocalDate reconciledDate;

    private Long reconciledBy;

    /**
     * 对账状态 0-未对账 1-已对账
     */
    private Integer status;

    private String remark;
}
