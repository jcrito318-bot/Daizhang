package com.company.daizhang.module.asset.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 资产月折旧趋势视图对象
 */
@Data
public class AssetDepreciationMonthlyVO {

    /**
     * 年度
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 月折旧额
     */
    private BigDecimal depreciationAmount;

    /**
     * 累计折旧
     */
    private BigDecimal accumulatedDepreciation;
}
