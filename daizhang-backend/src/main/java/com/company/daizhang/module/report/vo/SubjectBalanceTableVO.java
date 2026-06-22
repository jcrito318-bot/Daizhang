package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 科目余额表VO
 */
@Data
public class SubjectBalanceTableVO {

    /**
     * 科目余额明细（支持层级展开）
     */
    private List<SubjectBalanceRow> rows;

    /**
     * 期初借方合计
     */
    private BigDecimal totalBeginDebit;

    /**
     * 期初贷方合计
     */
    private BigDecimal totalBeginCredit;

    /**
     * 本期借方合计
     */
    private BigDecimal totalPeriodDebit;

    /**
     * 本期贷方合计
     */
    private BigDecimal totalPeriodCredit;

    /**
     * 期末借方合计
     */
    private BigDecimal totalEndDebit;

    /**
     * 期末贷方合计
     */
    private BigDecimal totalEndCredit;

    /**
     * 试算平衡标志（期初借方=期初贷方 且 本期借方=本期贷方 且 期末借方=期末贷方）
     */
    private boolean trialBalanceCheck;
}
