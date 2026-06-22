package com.company.daizhang.module.salary.dto;

import lombok.Data;

/**
 * 员工查询请求
 */
@Data
public class EmployeeQueryRequest {

    private Long accountSetId;

    private String employeeCode;

    private String employeeName;

    private String department;

    private Integer status;

    private Integer pageNum = 1;

    private Integer pageSize = 10;
}
