package com.company.daizhang.module.salary.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.daizhang.common.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 工资条推送记录实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("salary_payslip_push")
public class PayslipPushRecord extends BaseEntity {

    /**
     * 薪资表ID
     */
    private Long salarySheetId;

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 年份
     */
    @TableField("`year`")
    private Integer year;

    /**
     * 月份
     */
    @TableField("`month`")
    private Integer month;

    /**
     * 推送方式: PDF/EMAIL/SMS
     */
    private String pushMethod;

    /**
     * 推送状态: 0-待推送 1-已推送 2-失败
     */
    private Integer pushStatus;

    /**
     * 推送时间
     */
    private LocalDateTime pushTime;

    /**
     * 生成的PDF文件路径
     */
    private String filePath;

    /**
     * 失败原因
     */
    private String errorMessage;
}
