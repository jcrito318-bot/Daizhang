package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 多栏账VO
 */
@Data
public class MultiColumnLedgerVO {

    private String subjectCode;

    private String subjectName;

    private BigDecimal beginDebit;

    private BigDecimal beginCredit;

    private List<MultiColumnLedgerDetailVO> details;

    /**
     * 各栏目合计
     */
    private List<MultiColumnTotalVO> totals;
}
