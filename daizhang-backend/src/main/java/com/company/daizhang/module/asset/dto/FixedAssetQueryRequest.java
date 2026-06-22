package com.company.daizhang.module.asset.dto;

import lombok.Data;

/**
 * 固定资产查询请求
 */
@Data
public class FixedAssetQueryRequest {

    private Long accountSetId;

    private String assetCode;

    private String assetName;

    private Long categoryId;

    private Integer status;

    private String department;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
