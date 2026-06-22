package com.company.daizhang.module.bank.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 银行流水实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("bank_transaction")
public class BankTransaction extends BaseEntity {

    private Long accountSetId;

    private String bankAccount;

    private LocalDate transactionDate;

    /**
     * 交易类型 1-收入 2-支出
     */
    private Integer transactionType;

    private BigDecimal amount;

    private BigDecimal balance;

    private String counterparty;

    private String summary;

    private String transactionNo;

    /**
     * 匹配状态 0-未匹配 1-已匹配
     */
    private Integer matchedStatus;

    private Long voucherId;

    private String remark;
}
