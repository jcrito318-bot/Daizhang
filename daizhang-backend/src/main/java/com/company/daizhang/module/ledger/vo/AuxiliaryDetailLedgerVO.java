package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 辅助核算明细账VO
 */
@Data
public class AuxiliaryDetailLedgerVO {

    private String subjectCode;

    private String subjectName;

    /**
     * 核算类别名
     */
    private String auxiliaryCategoryName;

    /**
     * 核算项目名
     */
    private String auxiliaryItemName;

    private BigDecimal beginDebit;

    private BigDecimal beginCredit;

    private List<AuxiliaryDetailLedgerDetailVO> details;

    private BigDecimal endDebit;

    private BigDecimal endCredit;
}
