package com.company.daizhang.module.asset.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

/**
 * 资产分类查询请求
 */
@Data
public class AssetCategoryQueryRequest {

    private Long accountSetId;

    private String categoryCode;

    private String categoryName;

    private String depreciationMethod;

    private Long parentId;

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 10;
}
