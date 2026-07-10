package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 辅助核算余额表VO
 * 按"科目 + 辅助核算项"维度展示期初/本期发生/期末借贷方余额
 */
@Data
public class AuxiliaryBalanceVO {

    private String subjectCode;

    private String subjectName;

    /**
     * 辅助核算类别ID
     */
    private Long auxiliaryCategoryId;

    /**
     * 核算类别名
     */
    private String auxiliaryCategoryName;

    /**
     * 辅助核算项目ID
     */
    private Long auxiliaryId;

    /**
     * 核算项目名
     */
    private String auxiliaryItemName;

    /**
     * 期初借方
     */
    private BigDecimal beginDebit;

    /**
     * 期初贷方
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
     * 期末借方
     */
    private BigDecimal endDebit;

    /**
     * 期末贷方
     */
    private BigDecimal endCredit;

    /**
     * 余额方向: 借/贷
     */
    private String balanceDirection;
}
