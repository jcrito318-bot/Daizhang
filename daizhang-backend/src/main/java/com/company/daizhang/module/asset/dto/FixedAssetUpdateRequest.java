package com.company.daizhang.module.asset.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 固定资产更新请求
 */
@Data
public class FixedAssetUpdateRequest {

    @NotBlank(message = "资产名称不能为空")
    private String assetName;

    private Long categoryId;

    private String categoryName;

    private String depreciationMethod;

    @Min(value = 1, message = "使用年限必须大于0")
    private Integer usefulLife;

    @DecimalMin(value = "0", message = "残值不能为负数")
    @Digits(integer = 15, fraction = 2, message = "金额精度超出范围")
    private BigDecimal residualValue;

    private String department;

    private String keeper;

    private String remark;
}
