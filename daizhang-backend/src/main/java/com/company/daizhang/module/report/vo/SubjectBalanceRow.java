package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 科目余额表行
 */
@Data
public class SubjectBalanceRow {

    /**
     * 科目ID
     */
    private Long subjectId;

    /**
     * 科目编码
     */
    private String subjectCode;

    /**
     * 科目名称
     */
    private String subjectName;

    /**
     * 科目级次
     */
    private Integer level;

    /**
     * 余额方向 1-借 2-贷
     */
    private Integer balanceDirection;

    /**
     * 是否有子科目
     */
    private boolean hasChildren;

    /**
     * 期初借方余额
     */
    private BigDecimal beginDebit;

    /**
     * 期初贷方余额
     */
    private BigDecimal beginCredit;

    /**
     * 本期借方发生额
     */
    private BigDecimal periodDebit;

    /**
     * 本期贷方发生额
     */
    private BigDecimal periodCredit;

    /**
     * 期末借方余额
     */
    private BigDecimal endDebit;

    /**
     * 期末贷方余额
     */
    private BigDecimal endCredit;

    /**
     * 本年累计借方
     */
    private BigDecimal yearDebit;

    /**
     * 本年累计贷方
     */
    private BigDecimal yearCredit;
}
