package com.company.daizhang.module.salary.dto;

import lombok.Data;

/**
 * 个税专项附加扣除查询请求
 */
@Data
public class SpecialDeductionQueryRequest {

    private Long accountSetId;

    private Long employeeId;

    private String employeeName;

    private String deductionType;

    private Integer status;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
