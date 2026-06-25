package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 所有者权益变动表VO
 */
@Data
public class EquityChangeStatementVO {

    private Integer year;

    private Integer month;

    /**
     * 变动项目列表（实收资本、资本公积、盈余公积、未分配利润等）
     */
    private List<EquityChangeItem> items;

    /**
     * 年初余额合计
     */
    private BigDecimal totalBeginningBalance;

    /**
     * 本年增加合计
     */
    private BigDecimal totalIncrease;

    /**
     * 本年减少合计
     */
    private BigDecimal totalDecrease;

    /**
     * 期末余额合计
     */
    private BigDecimal totalEndingBalance;
}
