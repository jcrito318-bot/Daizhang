package com.company.daizhang.module.asset.dto;

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

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
