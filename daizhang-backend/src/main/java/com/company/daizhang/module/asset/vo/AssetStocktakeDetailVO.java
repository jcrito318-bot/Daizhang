package com.company.daizhang.module.asset.vo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 资产盘点明细视图对象
 */
@Data
public class AssetStocktakeDetailVO {

    private Long id;

    private Long stocktakeId;

    private Long assetId;

    private String assetCode;

    private String assetName;

    private BigDecimal bookQuantity;

    private BigDecimal actualQuantity;

    private BigDecimal diffQuantity;

    private BigDecimal bookValue;

    private BigDecimal actualValue;

    private BigDecimal diffAmount;

    private String result;

    private String resultDesc;

    private String handleOpinion;
}
