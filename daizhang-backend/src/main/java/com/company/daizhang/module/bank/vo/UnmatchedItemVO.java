package com.company.daizhang.module.bank.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 未达账项视图对象
 */
@Data
public class UnmatchedItemVO {

    /**
     * 银行流水ID
     */
    private Long transactionId;

    /**
     * 交易日期
     */
    private LocalDate transactionDate;

    /**
     * 金额
     */
    private BigDecimal amount;

    /**
     * 摘要
     */
    private String summary;

    /**
     * 类型: 收入/支出
     */
    private String type;
}
