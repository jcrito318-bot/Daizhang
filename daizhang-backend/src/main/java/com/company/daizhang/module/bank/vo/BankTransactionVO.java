package com.company.daizhang.module.bank.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 银行流水视图对象
 */
@Data
public class BankTransactionVO {

    private Long id;

    private Long accountSetId;

    private String bankAccount;

    private LocalDate transactionDate;

    private Integer transactionType;

    private String transactionTypeName;

    private BigDecimal amount;

    private BigDecimal balance;

    private String counterparty;

    private String summary;

    private String transactionNo;

    private Integer matchedStatus;

    private String matchedStatusName;

    private Long voucherId;

    private String voucherNo;

    private String remark;

    private Long createBy;

    private String createByName;

    private LocalDateTime createTime;
}
