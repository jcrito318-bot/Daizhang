package com.company.daizhang.module.industrycommerce.vo;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 工商外勤任务视图对象
 */
@Data
public class IndustryCommerceTaskVO {

    private Long id;

    /**
     * 工商服务ID
     */
    private Long serviceId;

    /**
     * 任务名称
     */
    private String taskName;

    /**
     * 任务状态:0待处理 1进行中 2已完成 3已取消
     */
    private Integer taskStatus;

    /**
     * 经办人ID
     */
    private Long assigneeId;

    /**
     * 外勤日期
     */
    private LocalDate fieldDate;

    /**
     * 地点
     */
    private String location;

    /**
     * 备注
     */
    private String remark;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;

    /**
     * 经办人名称
     */
    private String assigneeName;

    private Long createBy;

    private LocalDateTime createTime;

    private Long updateBy;

    private LocalDateTime updateTime;
}
