package com.company.daizhang.module.salary.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 工资条推送记录视图对象
 */
@Data
public class PayslipPushRecordVO {

    private Long id;

    private Long salarySheetId;

    private Long employeeId;

    private String employeeName;

    private Long accountSetId;

    private Integer year;

    private Integer month;

    private String pushMethod;

    private Integer pushStatus;

    private LocalDateTime pushTime;

    private String filePath;

    private String errorMessage;

    private Long createBy;

    private LocalDateTime createTime;
}
