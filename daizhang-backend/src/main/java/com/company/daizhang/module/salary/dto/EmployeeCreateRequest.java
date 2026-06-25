package com.company.daizhang.module.salary.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

/**
 * 员工创建请求
 */
@Data
public class EmployeeCreateRequest {

    @NotNull(message = "账套ID不能为空")
    private Long accountSetId;

    @NotBlank(message = "员工编号不能为空")
    private String employeeCode;

    @NotBlank(message = "员工姓名不能为空")
    private String employeeName;

    private String department;

    private String position;

    private String idCard;

    private String phone;

    private String bankName;

    private String bankAccount;

    private LocalDate entryDate;

    private Integer status;

    private String remark;
}
