package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 多栏账栏目合计VO
 */
@Data
public class MultiColumnTotalVO {

    /**
     * 栏目名
     */
    private String columnName;

    /**
     * 借方合计
     */
    private BigDecimal totalDebit;

    /**
     * 贷方合计
     */
    private BigDecimal totalCredit;
}
