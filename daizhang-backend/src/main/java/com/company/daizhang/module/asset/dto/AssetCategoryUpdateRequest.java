package com.company.daizhang.module.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 资产分类更新请求
 */
@Data
public class AssetCategoryUpdateRequest {

    @NotBlank(message = "分类名称不能为空")
    private String categoryName;

    @NotBlank(message = "折旧方法不能为空")
    private String depreciationMethod;

    @NotNull(message = "使用年限不能为空")
    private Integer usefulLife;

    @NotNull(message = "残值率不能为空")
    private BigDecimal residualRate;

    private Long parentId;

    private String remark;
}
