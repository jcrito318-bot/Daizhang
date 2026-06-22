package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 明细账VO
 */
@Data
public class DetailLedgerVO {

    private LocalDate voucherDate;

    private String voucherNo;

    private String summary;

    private String subjectCode;

    private String subjectName;

    private BigDecimal debit;

    private BigDecimal credit;

    private String direction;

    private BigDecimal balance;
}
