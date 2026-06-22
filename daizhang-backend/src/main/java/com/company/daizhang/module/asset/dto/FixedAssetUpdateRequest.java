package com.company.daizhang.module.asset.dto;

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

    private Integer usefulLife;

    private BigDecimal residualValue;

    private String department;

    private String keeper;

    private String remark;
}
