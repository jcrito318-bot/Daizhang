package com.company.daizhang.module.biz.vo;

import lombok.Data;

/**
 * 员工工作负荷统计VO
 */
@Data
public class EmployeeWorkloadVO {

    /**
     * 员工ID
     */
    private Long employeeId;

    /**
     * 员工姓名
     */
    private String employeeName;

    /**
     * 总任务数
     */
    private Integer totalTaskCount;

    /**
     * 待处理任务数（taskStatus=0）
     */
    private Integer pendingTaskCount;

    /**
     * 进行中任务数（taskStatus=1）
     */
    private Integer inProgressTaskCount;

    /**
     * 已完成任务数（taskStatus=2）
     */
    private Integer completedTaskCount;

    /**
     * 逾期待办数（创建超过7天且未完成）
     */
    private Integer overdueTaskCount;

    /**
     * 按时完成率（%）
     */
    private Double onTimeRate;

    /**
     * 平均完成时长（小时）
     */
    private Double avgCompleteHours;
}
