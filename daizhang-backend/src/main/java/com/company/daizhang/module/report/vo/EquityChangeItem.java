package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 所有者权益变动表项目
 */
@Data
public class EquityChangeItem {

    /**
     * 项目名称
     */
    private String itemName;

    /**
     * 行次
     */
    private Integer rowNo;

    /**
     * 本年年初余额
     */
    private BigDecimal beginningBalance;

    /**
     * 本年增加额
     */
    private BigDecimal increaseAmount;

    /**
     * 本年减少额
     */
    private BigDecimal decreaseAmount;

    /**
     * 本年期末余额
     */
    private BigDecimal endingBalance;
}
