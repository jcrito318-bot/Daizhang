package com.company.daizhang.module.tax.dto;

import lombok.Data;

/**
 * 税务申报查询请求
 */
@Data
public class TaxDeclarationQueryRequest {

    private Long accountSetId;

    private Integer year;

    private Integer month;

    private String taxType;

    private Integer status;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
