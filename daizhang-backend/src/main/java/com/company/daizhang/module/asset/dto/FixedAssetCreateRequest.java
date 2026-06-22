package com.company.daizhang.module.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 固定资产创建请求
 */
@Data
public class FixedAssetCreateRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotBlank(message = "资产编码不能为空")
    private String assetCode;

    @NotBlank(message = "资产名称不能为空")
    private String assetName;

    @NotNull(message = "分类ID不能为空")
    private Long categoryId;

    private String categoryName;

    @NotNull(message = "购入日期不能为空")
    private LocalDate purchaseDate;

    @NotNull(message = "购入金额不能为空")
    private BigDecimal purchaseAmount;

    /**
     * 折旧方法：直线法/工作量法/双倍余额递减法
     */
    @NotBlank(message = "折旧方法不能为空")
    private String depreciationMethod;

    /**
     * 使用年限（月）
     */
    @NotNull(message = "使用年限不能为空")
    private Integer usefulLife;

    /**
     * 残值
     */
    @NotNull(message = "残值不能为空")
    private BigDecimal residualValue;

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
