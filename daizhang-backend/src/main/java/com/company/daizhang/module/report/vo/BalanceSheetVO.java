package com.company.daizhang.module.report.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资产负债表VO
 */
@Data
public class BalanceSheetVO {

    /**
     * 资产项目
     */
    private List<BalanceSheetItem> assets;

    /**
     * 资产合计
     */
    private BigDecimal totalAssets;

    /**
     * 负债项目
     */
    private List<BalanceSheetItem> liabilities;

    /**
     * 负债合计
     */
    private BigDecimal totalLiabilities;

    /**
     * 所有者权益项目
     */
    private List<BalanceSheetItem> equity;

    /**
     * 所有者权益合计
     */
    private BigDecimal totalEquity;

    /**
     * 负债和所有者权益总计
     */
    private BigDecimal totalLiabilitiesAndEquity;

    /**
     * 是否平衡（资产=负债+所有者权益）
     */
    private boolean balanceCheck;
}
