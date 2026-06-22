package com.company.daizhang.module.salary.dto;

import lombok.Data;

import java.time.LocalDate;

/**
 * 员工更新请求
 */
@Data
public class EmployeeUpdateRequest {

    private String employeeCode;

    private String employeeName;

    private String department;

    private String position;

    private String idCard;

    private String phone;

    private LocalDate entryDate;

    private Integer status;

    private String remark;
}
