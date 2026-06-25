package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 现金流量表明细项
 */
@Data
public class CashFlowItemVO {

    private String category; // 经营/投资/筹资

    private String itemName;  // 项目名称

    private BigDecimal amount;
}
