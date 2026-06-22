package com.company.daizhang.module.salary.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 员工视图对象
 */
@Data
public class EmployeeVO {

    private Long id;

    private Long accountSetId;

    private String employeeCode;

    private String employeeName;

    private String department;

    private String position;

    private String idCard;

    private String phone;

    private LocalDate entryDate;

    private Integer status;

    private String remark;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;

    private String createByName;
}
