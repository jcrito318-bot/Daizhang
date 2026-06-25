package com.company.daizhang.module.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 资产盘点明细实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ast_stocktake_detail")
public class AssetStocktakeDetail extends BaseEntity {

    /**
     * 盘点单ID
     */
    private Long stocktakeId;

    /**
     * 固定资产ID
     */
    private Long assetId;

    /**
     * 资产编码
     */
    private String assetCode;

    /**
     * 资产名称
     */
    private String assetName;

    /**
     * 账面数量
     */
    private BigDecimal bookQuantity;

    /**
     * 实盘数量
     */
    private BigDecimal actualQuantity;

    /**
     * 差异数量（实盘-账面）
     */
    private BigDecimal diffQuantity;

    /**
     * 账面原值
     */
    private BigDecimal bookValue;

    /**
     * 实盘原值
     */
    private BigDecimal actualValue;

    /**
     * 差异金额
     */
    private BigDecimal diffAmount;

    /**
     * 盘点结果：MATCH-一致 LOSS-盘亏 GAIN-盘盈
     */
    private String result;

    /**
     * 处理意见
     */
    private String handleOpinion;
}
