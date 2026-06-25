package com.company.daizhang.module.asset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 资产盘点单实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ast_stocktake")
public class AssetStocktake extends BaseEntity {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 盘点单号
     */
    private String stocktakeNo;

    /**
     * 盘点单名称
     */
    private String stocktakeName;

    /**
     * 盘点日期
     */
    private LocalDate stocktakeDate;

    /**
     * 盘点人员
     */
    private String stocktakePerson;

    /**
     * 盘点范围：ALL-全部资产 CATEGORY-按分类 SPECIFIC-指定资产
     */
    private String scope;

    /**
     * 盘点状态 0-进行中 1-已完成 2-已作废
     */
    private Integer status;

    /**
     * 资产总数
     */
    private Integer totalCount;

    /**
     * 盘亏数量
     */
    private Integer lossCount;

    /**
     * 盘盈数量
     */
    private Integer gainCount;

    /**
     * 一致数量
     */
    private Integer matchCount;

    /**
     * 备注
     */
    private String remark;
}
