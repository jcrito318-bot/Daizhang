package com.company.daizhang.module.salary.dto;

import lombok.Data;

/**
 * 薪资表查询请求
 */
@Data
public class SalarySheetQueryRequest {

    private Long accountSetId;

    private Integer year;

    private Integer month;

    private Long employeeId;

    private String employeeName;

    private Integer status;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
