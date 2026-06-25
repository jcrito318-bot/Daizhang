package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 多栏账明细VO
 */
@Data
public class MultiColumnLedgerDetailVO {

    private LocalDate voucherDate;

    private String voucherNo;

    private String summary;

    /**
     * 栏目名
     */
    private String columnName;

    private BigDecimal debit;

    private BigDecimal credit;

    private BigDecimal balance;
}
