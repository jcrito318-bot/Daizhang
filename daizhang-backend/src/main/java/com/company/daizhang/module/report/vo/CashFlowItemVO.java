package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 现金流量表明细项
 */
@Data
public class CashFlowItemVO {

    /**
     * 项目编码（对应 CashFlowItem 枚举名，如 SALES_RECEIPTS）
     */
    private String itemCode;

    /**
     * 项目名称
     */
    private String itemName;

    /**
     * 分类：operating(经营) / investing(投资) / financing(筹资) / other(其他) / summary(汇总)
     * 兼容旧字段：经营 / 投资 / 筹资
     */
    private String category;

    /**
     * 方向：inflow(流入) / outflow(流出) / net(净额) / balance(余额)
     */
    private String direction;

    /**
     * 金额
     */
    private BigDecimal amount;
}
