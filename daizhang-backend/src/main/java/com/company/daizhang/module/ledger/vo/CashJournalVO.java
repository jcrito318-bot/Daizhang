package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 现金日记账VO
 */
@Data
public class CashJournalVO {

    private LocalDate voucherDate;

    private String voucherNo;

    private String summary;

    private BigDecimal income;

    private BigDecimal expense;

    private BigDecimal balance;
}
