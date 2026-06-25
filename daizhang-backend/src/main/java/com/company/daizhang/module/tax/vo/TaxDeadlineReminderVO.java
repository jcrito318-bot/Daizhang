package com.company.daizhang.module.tax.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 申报到期提醒视图对象
 */
@Data
public class TaxDeadlineReminderVO {

    /**
     * 账套ID
     */
    private Long accountSetId;

    /**
     * 账套名称
     */
    private String accountSetName;

    /**
     * 年度
     */
    private Integer year;

    /**
     * 月份
     */
    private Integer month;

    /**
     * 税种类型
     */
    private String taxType;

    /**
     * 申报截止日
     */
    private LocalDate deadline;

    /**
     * 剩余天数
     */
    private Integer daysRemaining;

    /**
     * 状态：未申报/已申报/已逾期
     */
    private String status;
}
