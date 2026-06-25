package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 辅助核算明细账明细VO
 */
@Data
public class AuxiliaryDetailLedgerDetailVO {

    private LocalDate voucherDate;

    private String voucherNo;

    private String summary;

    private BigDecimal debit;

    private BigDecimal credit;

    private String direction;

    private BigDecimal balance;
}
