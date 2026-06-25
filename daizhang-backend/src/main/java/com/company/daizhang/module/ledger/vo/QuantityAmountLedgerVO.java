package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 数量金额账VO
 */
@Data
public class QuantityAmountLedgerVO {

    private String subjectCode;

    private String subjectName;

    private String unit;

    private BigDecimal beginQuantity;

    private BigDecimal beginAmount;

    private List<QuantityAmountLedgerDetailVO> details;

    private BigDecimal endQuantity;

    private BigDecimal endAmount;
}
