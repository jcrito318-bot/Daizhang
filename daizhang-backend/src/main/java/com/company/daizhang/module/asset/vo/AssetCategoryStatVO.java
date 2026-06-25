package com.company.daizhang.module.asset.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 资产按分类统计视图对象
 */
@Data
public class AssetCategoryStatVO {

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 资产数量
     */
    private Integer assetCount;

    /**
     * 原值合计
     */
    private BigDecimal originalValue;

    /**
     * 累计折旧
     */
    private BigDecimal accumulatedDepreciation;

    /**
     * 净值合计
     */
    private BigDecimal netValue;
}
