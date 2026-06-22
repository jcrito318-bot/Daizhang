package com.company.daizhang.module.asset.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 资产分类视图对象
 */
@Data
public class AssetCategoryVO {

    private Long id;

    private Long accountSetId;

    private String categoryCode;

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
    private BigDecimal residualRate;

    private Long parentId;

    private String parentName;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
