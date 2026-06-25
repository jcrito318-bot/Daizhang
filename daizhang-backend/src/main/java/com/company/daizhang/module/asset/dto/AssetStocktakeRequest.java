package com.company.daizhang.module.asset.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 资产盘点单创建请求
 */
@Data
public class AssetStocktakeRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotBlank(message = "盘点单名称不能为空")
    private String stocktakeName;

    private LocalDate stocktakeDate;

    private String stocktakePerson;

    /**
     * 盘点范围：ALL-全部资产 CATEGORY-按分类 SPECIFIC-指定资产
     */
    private String scope;

    /**
     * 当scope=CATEGORY时，指定资产分类ID
     */
    private Long categoryId;

    /**
     * 当scope=SPECIFIC时，指定资产ID列表
     */
    private List<Long> assetIds;

    private String remark;
}
