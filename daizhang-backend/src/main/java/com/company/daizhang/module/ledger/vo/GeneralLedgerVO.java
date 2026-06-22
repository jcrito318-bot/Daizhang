package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 总账VO
 */
@Data
public class GeneralLedgerVO {

    private String subjectCode;

    private String subjectName;

    private BigDecimal beginDebit;

    private BigDecimal beginCredit;

    private BigDecimal periodDebit;

    private BigDecimal periodCredit;

    private BigDecimal endDebit;

    private BigDecimal endCredit;
}
