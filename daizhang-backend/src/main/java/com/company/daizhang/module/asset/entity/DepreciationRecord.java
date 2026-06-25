package com.company.daizhang.module.asset.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 折旧记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_depreciation_record")
public class DepreciationRecord extends BaseEntity {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 资产ID
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
     * 年度
     */
    @TableField("`year`")
    private Integer year;

    /**
     * 月份
     */
    @TableField("`month`")
    private Integer month;

    /**
     * 折旧金额
     */
    private BigDecimal depreciationAmount;

    /**
     * 累计折旧
     */
    private BigDecimal accumulatedDepreciation;

    /**
     * 净值
     */
    private BigDecimal netValue;

    /**
     * 凭证ID
     */
    private Long voucherId;

    /**
     * 备注
     */
    private String remark;
}
