package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 数量金额账明细VO
 */
@Data
public class QuantityAmountLedgerDetailVO {

    private LocalDate voucherDate;

    private String voucherNo;

    private String summary;

    /**
     * 数量
     */
    private BigDecimal quantity;

    /**
     * 单价
     */
    private BigDecimal unitPrice;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 借/贷
     */
    private String direction;

    private BigDecimal balanceQuantity;

    private BigDecimal balanceAmount;
}
