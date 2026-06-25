package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 往来对账明细VO
 */
@Data
public class ReconciliationDetailVO {

    private LocalDate voucherDate;

    private String voucherNo;

    private String summary;

    private BigDecimal debit;

    private BigDecimal credit;

    /**
     * 是否已核对
     */
    private Boolean matched;
}
