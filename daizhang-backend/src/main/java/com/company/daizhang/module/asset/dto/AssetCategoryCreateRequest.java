package com.company.daizhang.module.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 资产分类创建请求
 */
@Data
public class AssetCategoryCreateRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotBlank(message = "分类编码不能为空")
    private String categoryCode;

    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

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
     * 残值率（%）
     */
    @NotNull(message = "残值率不能为空")
    private BigDecimal residualRate;

    /**
     * 父分类ID
     */
    private Long parentId;

    /**
     * 备注
     */
    private String remark;
}
