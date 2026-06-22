package com.company.daizhang.module.asset.dto;

import lombok.Data;

/**
 * 折旧记录查询请求
 */
@Data
public class DepreciationRecordQueryRequest {

    private Long accountSetId;

    private Long assetId;

    private String assetCode;

    private String assetName;

    private Integer year;

    private Integer month;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
