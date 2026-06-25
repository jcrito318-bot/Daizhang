package com.company.daizhang.module.asset.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 资产报表视图对象
 */
@Data
public class AssetReportVO {

    /**
     * 资产总数
     */
    private Integer totalAssets;

    /**
     * 原值合计
     */
    private BigDecimal totalOriginalValue;

    /**
     * 累计折旧
     */
    private BigDecimal totalAccumulatedDepreciation;

    /**
     * 净值合计
     */
    private BigDecimal totalNetValue;

    /**
     * 按分类统计
     */
    private List<AssetCategoryStatVO> categoryStats;

    /**
     * 月折旧趋势
     */
    private List<AssetDepreciationMonthlyVO> monthlyDepreciations;
}
