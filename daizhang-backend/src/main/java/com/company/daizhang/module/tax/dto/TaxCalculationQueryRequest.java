package com.company.daizhang.module.tax.dto;

import lombok.Data;

/**
 * 税务计算查询请求
 */
@Data
public class TaxCalculationQueryRequest {

    private Long accountSetId;

    private Integer year;

    private Integer month;

    private String taxType;

    private String calculationItem;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
