package com.company.daizhang.module.asset.dto;

import lombok.Data;

/**
 * 资产盘点单查询请求
 */
@Data
public class AssetStocktakeQueryRequest {

    private Long accountSetId;

    private String stocktakeNo;

    private String stocktakeName;

    private Integer status;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
