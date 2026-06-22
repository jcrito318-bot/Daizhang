package com.company.daizhang.module.asset.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 固定资产视图对象
 */
@Data
public class FixedAssetVO {

    private Long id;

    private Long accountSetId;

    private String assetCode;

    private String assetName;

    private Long categoryId;

    private String categoryName;

    private LocalDate purchaseDate;

    private BigDecimal purchaseAmount;

    /**
     * 折旧方法：直线法/工作量法/双倍余额递减法
     */
    private String depreciationMethod;

    /**
     * 使用年限（月）
     */
    private Integer usefulLife;

    private BigDecimal residualValue;

    private BigDecimal monthlyDepreciation;

    private BigDecimal accumulatedDeprecation;

    private BigDecimal netValue;

    /**
     * 状态：0-在用 1-闲置 2-报废
     */
    private Integer status;

    private String statusName;

    private String department;

    private String keeper;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
