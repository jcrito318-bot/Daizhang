package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 账龄分析明细项VO(单个客户/供应商)
 * <p>
 * 一行对应一个客户(应收)或一个供应商(应付)的账龄分布:
 * <ul>
 *   <li>totalAmount: 该客户/供应商截至 asOfDate 的未核销余额合计</li>
 *   <li>ageBuckets: 按凭证日期分桶后的金额分布</li>
 *   <li>oldestDate: 最早未核销凭证日期(用于计算最长逾期天数)</li>
 *   <li>voucherCount: 涉及凭证条数</li>
 * </ul>
 */
@Data
public class AgingItemVO {

    /**
     * 客户/供应商ID
     */
    private Long customerId;

    /**
     * 客户/供应商名称
     */
    private String customerName;

    /**
     * 总金额(未核销余额合计)
     */
    private BigDecimal totalAmount;

    /**
     * 账龄分桶金额
     */
    private AgeBucketVO ageBuckets;

    /**
     * 最早凭证日期(用于计算最长逾期天数)
     */
    private LocalDate oldestDate;

    /**
     * 最长逾期天数(从最早凭证日期到截止日期)
     */
    private Integer oldestDays;

    /**
     * 涉及凭证条数
     */
    private Integer voucherCount;
}
