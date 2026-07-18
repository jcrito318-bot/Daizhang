package com.company.daizhang.module.asset.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
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
    @Min(value = 1, message = "使用年限必须大于0")
    private Integer usefulLife;

    @NotNull(message = "残值率不能为空")
    @DecimalMin(value = "0", message = "残值率不能为负数")
    @DecimalMax(value = "1", message = "残值率不能超过100%")
    @Digits(integer = 1, fraction = 4, message = "残值率精度超出范围")
    private BigDecimal residualRate;

    private Long parentId;

    private String remark;
}
