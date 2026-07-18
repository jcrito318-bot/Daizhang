package com.company.daizhang.module.tax.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
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

    @Min(value = 1, message = "页码不能小于1")
    private Integer pageNum = 1;

    @Min(value = 1, message = "每页条数不能小于1")
    @Max(value = 100, message = "每页条数不能超过100")
    private Integer pageSize = 10;
}
