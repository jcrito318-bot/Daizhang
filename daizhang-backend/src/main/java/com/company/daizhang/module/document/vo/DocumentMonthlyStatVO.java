package com.company.daizhang.module.document.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 票据按月统计视图对象
 */
@Data
public class DocumentMonthlyStatVO {

    /**
     * 年度
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 数量
     */
    private Integer count;

    /**
     * 金额合计
     */
    private BigDecimal totalAmount;
}
