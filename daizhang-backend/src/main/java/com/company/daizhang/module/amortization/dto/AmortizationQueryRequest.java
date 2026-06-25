package com.company.daizhang.module.amortization.dto;

import lombok.Data;

/**
 * 长期待摊费用查询请求
 */
@Data
public class AmortizationQueryRequest {

    private Long accountSetId;

    private String amortizationName;

    private Integer status;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
