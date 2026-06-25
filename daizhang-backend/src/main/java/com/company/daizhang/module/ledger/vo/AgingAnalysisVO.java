package com.company.daizhang.module.ledger.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 账龄分析VO
 */
@Data
public class AgingAnalysisVO {

    private String subjectCode;

    private String subjectName;

    /**
     * 客户/供应商名
     */
    private String auxiliaryItemName;

    /**
     * 总金额
     */
    private BigDecimal totalAmount;

    /**
     * 未到期
     */
    private BigDecimal notDueAmount;

    /**
     * 0-30天
     */
    private BigDecimal due0to30;

    /**
     * 31-60天
     */
    private BigDecimal due31to60;

    /**
     * 61-90天
     */
    private BigDecimal due61to90;

    /**
     * 91-180天
     */
    private BigDecimal due91to180;

    /**
     * 180天以上
     */
    private BigDecimal dueOver180;

    /**
     * 风险等级
     */
    private String agingLevel;
}
