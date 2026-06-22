package com.company.daizhang.module.salary.dto;

import lombok.Data;

/**
 * 薪资项目查询请求
 */
@Data
public class SalaryItemQueryRequest {

    private Long accountSetId;

    private String itemName;

    private String itemCode;

    private String itemType;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
