package com.company.daizhang.module.asset.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 资产分类实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_category")
public class AssetCategory extends BaseEntity {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 分类编码
     */
    private String categoryCode;

    /**
     * 分类名称
     */
    private String categoryName;

    /**
     * 折旧方法：直线法/工作量法/双倍余额递减法
     */
    private String depreciationMethod;

    /**
     * 使用年限（月）
     */
    private Integer usefulLife;

    /**
     * 残值率（%）
     */
    private java.math.BigDecimal residualRate;

    /**
     * 父分类ID
     */
    private Long parentId;

    /**
     * 备注
     */
    private String remark;
}
