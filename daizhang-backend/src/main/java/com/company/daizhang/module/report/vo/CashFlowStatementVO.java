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

    // 经营活动
    private BigDecimal operatingInflow;   // 经营活动现金流入
    private BigDecimal operatingOutflow;  // 经营活动现金流出
    private BigDecimal operatingNetFlow;  // 经营活动现金净额

    // 投资活动
    private BigDecimal investingInflow;
    private BigDecimal investingOutflow;
    private BigDecimal investingNetFlow;

    // 筹资活动
    private BigDecimal financingInflow;
    private BigDecimal financingOutflow;
    private BigDecimal financingNetFlow;

    // 净增加额
    private BigDecimal netIncrease;

    // 本年累计(1~month)
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

    // 明细项列表
    private List<CashFlowItemVO> items;
}
