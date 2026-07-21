package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 现金流量表VO
 */
@Data
public class CashFlowStatementVO {

    private Integer year;

    private Integer month;

    /**
     * 账套ID
     */
    private Long accountSetId;

    // ===== 经营活动 =====
    private BigDecimal operatingInflow;   // 经营活动现金流入
    private BigDecimal operatingOutflow;  // 经营活动现金流出
    private BigDecimal operatingNetFlow;  // 经营活动现金净额

    // ===== 投资活动 =====
    private BigDecimal investingInflow;
    private BigDecimal investingOutflow;
    private BigDecimal investingNetFlow;

    // ===== 筹资活动 =====
    private BigDecimal financingInflow;
    private BigDecimal financingOutflow;
    private BigDecimal financingNetFlow;

    // ===== 净增加额 =====
    private BigDecimal netIncrease;

    /**
     * 经营活动产生的现金流量净额（与 operatingNetFlow 含义一致，命名遵循会计准则术语）
     */
    private BigDecimal operatingNetCashFlow;

    /**
     * 投资活动产生的现金流量净额
     */
    private BigDecimal investingNetCashFlow;

    /**
     * 筹资活动产生的现金流量净额
     */
    private BigDecimal financingNetCashFlow;

    /**
     * 现金及现金等价物净增加额（经营+投资+筹资+汇率变动影响）
     */
    private BigDecimal netIncreaseInCash;

    /**
     * 期初现金及现金等价物余额
     */
    private BigDecimal beginningCashBalance;

    /**
     * 期末现金及现金等价物余额
     */
    private BigDecimal endingCashBalance;

    /**
     * 汇率变动对现金的影响
     */
    private BigDecimal exchangeEffect;

    /**
     * 勾稽校验是否通过：净增加额 ≈ 期末 - 期初（允许0.01差异）
     */
    private Boolean balanceCheck;

    // ===== 本年累计(1~month) =====
    private BigDecimal operatingInflowYear;
    private BigDecimal operatingOutflowYear;
    private BigDecimal operatingNetFlowYear;
    private BigDecimal investingInflowYear;
    private BigDecimal investingOutflowYear;
    private BigDecimal investingNetFlowYear;
    private BigDecimal financingInflowYear;
    private BigDecimal financingOutflowYear;
    private BigDecimal financingNetFlowYear;
    private BigDecimal netIncreaseYear;

    // ===== 明细项列表 =====
    private List<CashFlowItemVO> items;
}
