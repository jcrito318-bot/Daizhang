package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 科目余额表VO
 */
@Data
public class SubjectBalanceVO {

    private String subjectCode;

    private String subjectName;

    private Integer level;

    private BigDecimal beginDebit;

    private BigDecimal beginCredit;

    private BigDecimal periodDebit;

    private BigDecimal periodCredit;

    private BigDecimal endDebit;

    private BigDecimal endCredit;

    private BigDecimal yearDebit;

    private BigDecimal yearCredit;
}
