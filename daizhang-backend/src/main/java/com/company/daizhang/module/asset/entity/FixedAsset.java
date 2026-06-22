package com.company.daizhang.module.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_fixed")
public class FixedAsset extends BaseEntity {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 资产编码
     */
    private String assetCode;

    /**
     * 资产名称
     */
    private String assetName;

    /**
     * 分类ID
     */
    private Long categoryId;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 购入日期
     */
    private LocalDate purchaseDate;

    /**
     * 购入金额
     */
    private BigDecimal purchaseAmount;

    /**
     * 折旧方法：直线法/工作量法/双倍余额递减法
     */
    private String depreciationMethod;

    /**
     * 使用年限（月）
     */
    private Integer usefulLife;

    /**
     * 残值
     */
    private BigDecimal residualValue;

    /**
     * 月折旧额
     */
    private BigDecimal monthlyDepreciation;

    /**
     * 累计折旧
     */
    private BigDecimal accumulatedDeprecation;

    /**
     * 净值
     */
    private BigDecimal netValue;

    /**
     * 状态：0-在用 1-闲置 2-报废
     */
    private Integer status;

    /**
     * 使用部门
     */
    private String department;

    /**
     * 保管人
     */
    private String keeper;

    /**
     * 备注
     */
    private String remark;
}
